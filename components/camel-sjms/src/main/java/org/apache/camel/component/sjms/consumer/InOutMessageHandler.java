/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.sjms.consumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.sjms.JmsMessageHelper;
import org.apache.camel.component.sjms.jms.JmsObjectFactory;
import org.apache.camel.spi.Synchronization;
import org.apache.camel.util.AsyncProcessorHelper;

/**
 * TODO Add Class documentation for DefaultMessageHandler
 * 
 * @author sully6768
 */
public class InOutMessageHandler extends DefaultMessageHandler {

    /**
     * TODO Add Constructor Javadoc
     * 
     * @param endpoint
     * @param processor
     */
    public InOutMessageHandler(Endpoint endpoint, AtomicBoolean stopped,
            ExecutorService executor) {
        this(endpoint, stopped, executor, null);
    }

    /**
     * TODO Add Constructor Javadoc
     * 
     * @param stopped
     * @param synchronization
     */
    public InOutMessageHandler(Endpoint endpoint, AtomicBoolean stopped,
            ExecutorService executor, Synchronization synchronization) {
        super(endpoint, stopped, executor, synchronization);
    }

    /**
     * @param message
     */
    @Override
    public void doHandleMessage(final Exchange exchange) {
        if (log.isDebugEnabled()) {
            log.debug("SjmsMessageConsumer invoked for Exchange id:{} ",
                    exchange.getExchangeId());
        }
        if (isStarted()) {
            MessageHanderAsyncCallback callback = new MessageHanderAsyncCallback(
                    exchange);
            if (exchange.isFailed()) {
                return;
            } else {
                if (isTransacted() || isSynchronous()) {
                    // must process synchronous if transacted or configured to
                    // do so
                    if (log.isTraceEnabled()) {
                        log.trace("Processing Exchange id:{} synchronously",
                                exchange.getExchangeId());
                    }
                    try {
                        AsyncProcessorHelper.process(getProcessor(), exchange);
                    } catch (Exception e) {
                        exchange.setException(e);
                    } finally {
                        callback.done(true);
                    }
                } else {
                    // process asynchronous using the async routing engine
                    if (log.isTraceEnabled()) {
                        log.trace("Processing Exchange id:{} asynchronously",
                                exchange.getExchangeId());
                    }
                    boolean sync = AsyncProcessorHelper.process(getProcessor(),
                            exchange, callback);
                    if (!sync) {
                        // will be done async so return now
                        return;
                    }
                }
            }
        } else {
            log.warn(
                    "SjmsMessageConsumer invoked while stopped.  Exchange id:{} will not be processed.",
                    exchange.getExchangeId());
        }

        if (log.isDebugEnabled()) {
            log.debug("SjmsMessageConsumer invoked for Exchange id:{} ",
                    exchange.getExchangeId());
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

            try {
                log.warn(
                        "Transacted Exchange id:{} failed.  Rolling back transaction.",
                        exchange.getExchangeId());
                Message response = JmsMessageHelper.createMessage(exchange,
                        getSession(), true);
                // TextMessage response = getSession().createTextMessage();
                // response.setJMSCorrelationID(request.getJMSCorrelationID());
                Object obj = exchange.getIn().getHeader("JMSReplyTo");
                Destination replyTo = null;
                if (obj instanceof String) {
                    replyTo = JmsObjectFactory.createQueue(getSession(),
                            (String) obj);
                } else {
                    replyTo = (Destination) obj;
                }
                response.setJMSCorrelationID(exchange.getIn().getHeader(
                        "JMSCorrelationID", String.class));
                MessageProducer mp = getSession().createProducer(replyTo);
                mp.send(response);
                mp.close();
            } catch (JMSException e) {
                throw new RuntimeCamelException(
                        "Unable to rollback the transaction. " + "Error: "
                                + e.getErrorCode() + " - "
                                + e.getLocalizedMessage(), e);
            } catch (Exception e) {
                log.error("TODO Auto-generated catch block", e);
            }
        }
    }
}
