/*
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.camel.component.sjms.messagehandlers;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.sjms.jms.JmsMessageHeaderType;
import org.apache.camel.util.AsyncProcessorHelper;

/**
 * TODO Add Class documentation for DefaultMessageHandler
 *
 * @author sully6768
 */
public class InOnlyMessageHandler extends DefaultMessageHandler {
    
    /**
     * TODO Add Constructor Javadoc
     *
     * @param endpoint
     * @param processor
     */
    public InOnlyMessageHandler(AtomicBoolean stopped) {
        super(stopped);
    }
    
    @Override
    public void onMessage(Message message) {
        handleMessage(message);
    }

    /**
     * @param message
     */
    @Override
    public void doHandleMessage(final Exchange exchange) {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("MessageHandler invoked for Exchange id:{} ", exchange.getExchangeId());
        }
        if (isStarted()) {
            MessageHanderAsyncCallback callback = new MessageHanderAsyncCallback(exchange);
            if (exchange.isFailed()) {
                return;
            } else {
                if (isTransacted() || !isAsync()) {
                    // must process synchronous if transacted or configured to do so
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Processing Exchange id:{} synchronously", exchange.getExchangeId());
                    }
                    try {
                        getProcessor().process(exchange);
                    } catch (Exception e) {
                        exchange.setException(e);
                    } finally {
                        callback.done(true);
                    }
                } else {
                    // process asynchronous using the async routing engine
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Processing Exchange id:{} asynchronously", exchange.getExchangeId());
                    }
                    boolean sync = AsyncProcessorHelper.process(getProcessor(), exchange, callback);
                    if (!sync) {
                        // will be done async so return now
                        return;
                    }
                }    
            }
        } else {
            LOGGER.warn("MessageHandler invoked while stopped.  Exchange id:{} will not be processed.", exchange.getExchangeId());
        }

        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("MessageHandler invoked for Exchange id:{} ", exchange.getExchangeId());
        }
    }
    
    protected class MessageHanderAsyncCallback implements AsyncCallback {
        
        private Exchange exchange;
        

        /**
         * TODO Add Constructor Javadoc
         *
         * @param xid
         * @param exchange
         */
        public MessageHanderAsyncCallback(Exchange exchange) {
            super();
            this.exchange = exchange;
        }             

        @Override
        public void done(boolean sync) {
            if (exchange.isFailed()) {

                if(LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Exchange id:{} failed.", exchange.getExchangeId());
                }
                
                if(isTransacted() ) {
                    if (getSession() == null) {
                        throw new RuntimeCamelException(
                                "Transacted exchange with null Session.  Unable to process failed Exchange id:" + exchange.getExchangeId());
                    } else {
                        try {
                            LOGGER.warn("Transacted Exchange id:{} failed.  Rolling back transaction.", 
                                    exchange.getExchangeId());
                            getSession().rollback();
                        } catch (JMSException e) {
                            throw new RuntimeCamelException(
                                    "Unable to rollback the transaction. " +
                                    "Error: " + e.getErrorCode() + " - " + e.getLocalizedMessage(), e);
                        }   
                    }
                } else {
                    LOGGER.warn("Non transacted Exchange id:{} failed.", exchange.getExchangeId());
                }
            } else {
                if(isTransacted()) {
                    if(getSession() != null) {
                        try {
                            if(LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Transacted Exchange  id:{} for JMS Message ID {} succeeded.  Committing transaction.", 
                                        exchange.getExchangeId(), 
                                        exchange.getIn().getHeader(JmsMessageHeaderType.JMSMessageID.toString()));
                            }
                            getSession().commit();
                            if(LOGGER.isDebugEnabled()) {
                                LOGGER.debug("--Commit Successful");
                            }
                        } catch (JMSException e) {
                            if(LOGGER.isDebugEnabled()) {
                                LOGGER.debug("--Commit Failed");
                            }
                            throw new RuntimeCamelException(
                                    "Unable to commit the transaction. " +
                                    "Error: " + e.getErrorCode() + " - " + e.getLocalizedMessage(), e);
                        }
                    }
                } else {
                    if(LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Non-transacted Exchange id:{} succeeded.  Return from callback.", 
                                exchange.getExchangeId());
                    }
                }
            }
        }
    }
}
