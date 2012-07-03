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
package org.apache.camel.component.sjms.consumer;

import static org.apache.camel.util.ObjectHelper.wrapRuntimeCamelException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.camel.AsyncProcessor;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.sjms.JmsMessageHelper;
import org.apache.camel.component.sjms.jms.SessionAcknowledgementType;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.spi.Synchronization;
import org.apache.camel.util.ObjectHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Add Class documentation for DefaultMessageHandler
 *
 * @author sully6768
 */
public class DefaultMessageHandler implements MessageListener {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private Endpoint endpoint;
    private AsyncProcessor processor;
    private Session session;
    private boolean transacted = false;
    private SessionAcknowledgementType acknowledgementType = SessionAcknowledgementType.AUTO_ACKNOWLEDGE;
    private boolean synchronous = true;
    private final AtomicBoolean stopped;
    private Destination namedReplyToDestination;
    private final ExecutorService executor;
    
    private Synchronization synchronization;
    
    /**
     * TODO Add Constructor Javadoc
     *
     * @param endpoint
     * @param processor
     */
    public DefaultMessageHandler(Endpoint endpoint, AtomicBoolean stopped, ExecutorService executor) {
        this(endpoint, stopped, executor, null);
    }
    
    /**
     * TODO Add Constructor Javadoc
     *
     * @param endpoint
     * @param processor
     */
    public DefaultMessageHandler(Endpoint endpoint, AtomicBoolean stopped, ExecutorService executor, Synchronization synchronization) {
        super();
        this.stopped = stopped;
        this.synchronization = synchronization;
        this.endpoint = endpoint;
        this.executor = executor;
    }
    
    @Override
    public void onMessage(Message message) {
        handleMessage(message);
    }
    
    /**
     * @param message
     */
    public void handleMessage(Message message) {
        RuntimeCamelException rce = null;
        try {
            final DefaultExchange exchange = (DefaultExchange) JmsMessageHelper.createExchange(message, getEndpoint());
            if(log.isDebugEnabled()) {
                log.debug("Processing Exchange.id:{}", exchange.getExchangeId());
            }
            if (isTransacted() && synchronization != null) {
                exchange.addOnCompletion(synchronization);
            }
            try {
                if(isTransacted() || isSynchronous()) {
                    if(log.isDebugEnabled()) {
                        log.debug("  Sending message synchronously for Exchange id:{}", exchange.getExchangeId());
                    }
                    doHandleMessage(exchange);
                } else {
                    if(log.isDebugEnabled()) {
                        log.debug("  Sending message asynchronously for Exchange id:{}", exchange.getExchangeId());
                    }
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                doHandleMessage(exchange);
                            } catch (Exception e) {
                                ObjectHelper.wrapRuntimeCamelException(e);
                            }
                            
                        }
                    });
                }
            } catch(Exception e) {
                if(exchange != null) {
                    if (exchange.getException() == null) {
                        exchange.setException(e);
                    } else {
                        throw e;
                    }
                }
            }
        } catch(Exception e) {
            rce = wrapRuntimeCamelException(e);
        } finally {
            if(rce != null) {
                throw rce;
            }
        }
    }
    
    public void doHandleMessage(final Exchange exchange) {}
    
    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }

    public boolean isTransacted() {
        return transacted;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public AsyncProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(AsyncProcessor processor) {
        this.processor = processor;
    }

    public SessionAcknowledgementType getAcknowledgementType() {
        return acknowledgementType;
    }

    public void setAcknowledgementType(
            SessionAcknowledgementType acknowledgementType) {
        this.acknowledgementType = acknowledgementType;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public void setSynchronous(boolean async) {
        this.synchronous = async;
    }

    public boolean isSynchronous() {
        return synchronous;
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public boolean isStarted() {
        return !isStopped();
    }

    public void setNamedReplyToDestination(Destination namedReplyToDestination) {
        this.namedReplyToDestination = namedReplyToDestination;
    }

    public Destination getNamedReplyToDestination() {
        return namedReplyToDestination;
    }
}
