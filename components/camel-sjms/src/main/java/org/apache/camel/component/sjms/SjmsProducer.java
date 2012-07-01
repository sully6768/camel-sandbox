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
package org.apache.camel.component.sjms;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.sjms.pool.ConnectionPool;
import org.apache.camel.component.sjms.pool.ObjectPool;
import org.apache.camel.impl.DefaultAsyncProducer;
import org.apache.camel.spi.Synchronization;

/**
 * TODO Add Class documentation for SjmsProducer
 *
 * @author sully6768
 */
public abstract class SjmsProducer extends DefaultAsyncProducer  {

    public class RobustInOnlyTask implements Runnable { 
        private Exchange exchange;
        private AsyncCallback callback;

        public RobustInOnlyTask(Exchange exchange, AsyncCallback callback) {
            this.exchange = exchange;
            this.callback = callback;
        }

        public void run() {
            try {
                if(log.isDebugEnabled()) {
                    log.debug("Executing RobustInOnlyTask on Exchange.id:{}", exchange.getExchangeId());
                }
                sendMessage(exchange);
            } catch (Exception e) {
                exchange.setException(e);
            }
            callback.done(false);
        }
    }

    public class InOutTask implements Runnable { 
        private Exchange exchange;
        private AsyncCallback callback;

        public InOutTask(Exchange exchange, AsyncCallback callback) {
            this.exchange = exchange;
            this.callback = callback;
        }

        public void run() {
            try {
                if(log.isDebugEnabled()) {
                    log.debug("Executing InOutTask on Exchange.id:{}", exchange.getExchangeId());
                }
                sendMessage(exchange);
            } catch (Exception e) {
                exchange.setException(e);
            }
            callback.done(false);
        }
    }

    public class InOnlyTask implements Runnable { 
        private Exchange exchange;
        private AsyncCallback callback;

        public InOnlyTask(Exchange exchange, AsyncCallback callback) {
            this.exchange = exchange;
            this.callback = callback;
        }
        
        public void execute() {
            try {
                if(log.isDebugEnabled()) {
                    log.debug("Executing InOnlyTask on Exchange.id:{}", exchange.getExchangeId());
                }
                sendMessage(exchange);
            } catch (Exception e) {
                exchange.setException(e);
            }
            callback.done(false);
        }

        public void run() {
            execute();
        }
    }
    
    protected class MessageProducerPool extends ObjectPool<MessageProducerModel>{

        /**
         * TODO Add Constructor Javadoc
         *
         * @param sessionPool
         */
        public MessageProducerPool() {
            super(getMaxProducers());
        }

        @Override
        protected MessageProducerModel createObject() throws Exception {
            return createProducerModel();
        }
        
        @Override
        protected void destroyObject(MessageProducerModel model) throws Exception {
            if (model.getMessageProducer() != null) {
                model.getMessageProducer().close();
            }
            
            if(model.getSession() != null) {
                if (model.getSession().getTransacted()) {
                    try {
                        model.getSession().rollback();
                    } catch (Exception e) {
                        // Do nothing.  Just make sure we are cleaned up
                    }
                }
                model.getSession().close();
            }
        }
    }
    
    protected class MessageProducerModel {
        private final Session session;
        private final MessageProducer messageProducer;

        /**
         * TODO Add Constructor Javadoc
         * 
         * @param session
         * @param messageProducer
         */
        public MessageProducerModel(Session session, MessageProducer messageProducer) {
            super();
            this.session = session;
            this.messageProducer = messageProducer;
        }

        /**
         * Gets the Session value of session for this instance of
         * MessageProducerModel.
         * 
         * @return the session
         */
        public Session getSession() {
            return session;
        }

        /**
         * Gets the QueueSender value of queueSender for this instance of
         * MessageProducerModel.
         * 
         * @return the queueSender
         */
        public MessageProducer getMessageProducer() {
            return messageProducer;
        }
    }
    
    private MessageProducerPool producers;
    private final ExecutorService executor;

    public SjmsProducer(Endpoint endpoint) {
        super(endpoint);
        this.executor = endpoint.getCamelContext().getExecutorServiceManager().newDefaultThreadPool(this, "QProducer");
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if(getProducers() == null) {
            setProducers(new MessageProducerPool());
            getProducers().fillPool();
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (getProducers() != null) {
            getProducers().drainPool();
            setProducers(null);   
        }
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        if (log.isDebugEnabled()) {
            log.info((String) exchange.getIn().getBody());
        }
        boolean syncProcessing = true;
        
        if(exchange.getPattern() == ExchangePattern.RobustInOnly) {
            // process process the In Message  
            // and in the case of an exception propagate it to the exchange
            syncProcessing = processRobustInOnly(exchange, callback);
        } else if(exchange.getPattern() == ExchangePattern.InOut) {
            // process request reply
            syncProcessing = processInOut(exchange, callback);
        } else {
            // Default is to process the InOnly pattern where
            // errors are not propagated to the Exchange
            syncProcessing = processInOnly(exchange, callback);
        }
        
        return syncProcessing;
    }
    
    protected boolean processInOnly(final Exchange exchange, final AsyncCallback callback) {
        boolean syncProcessing = false;
        if(log.isDebugEnabled()) {
            log.debug("Processing InOnly Exchange id:{}", exchange.getExchangeId());
        }
        try {
            if( ! isSyncronous()) {
                if(log.isDebugEnabled()) {
                    log.debug("Sending message asynchronously for Exchange id:{}", exchange.getExchangeId());
                }
                executor.execute(new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            sendMessage(exchange);
                        } catch (Exception e) {
                            throw new RuntimeCamelException(e);
                        }
                    }
                });
//                executor.submit(new Callable<Object>() {
//                    public Object call() throws Exception {
//                        sendMessage(exchange);
//                        return null;
//                    }
//                });
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("Sending message synchronously for Exchange id:{}", exchange.getExchangeId());
                }
                syncProcessing = true;
                sendMessage(exchange);
            }
        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.debug("Processing InOnly Exchange id:{}", exchange.getExchangeId() + " - Failed");
            }
            exchange.setException(e);
        }
        callback.done(syncProcessing);
        if(log.isDebugEnabled()) {
            log.debug("Processing InOnly Exchange id:{}", exchange.getExchangeId() + " - SUCCESS");
        }
        return syncProcessing;
    }
    
    protected boolean processRobustInOnly(final Exchange exchange, final AsyncCallback callback) {
        boolean syncProcessing = false;
        if(log.isDebugEnabled()) {
            log.debug("Processing Robust-InOnly Exchange id:{}", exchange.getExchangeId());
        }
        try {
            if(isSyncronous()) {
                if(log.isDebugEnabled()) {
                    log.debug("  Sending message asynchronously for Exchange id:{}", exchange.getExchangeId());
                }
                executor.submit(new Callable<Object>() {
                    public Object call() throws Exception {
                        sendMessage(exchange);
                        return null;
                    }
                });
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("  Sending message synchronously for Exchange id:{}", exchange.getExchangeId());
                }
                syncProcessing = true;
                sendMessage(exchange);
            }
        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.debug("Processing Robust-InOnly Exchange id:{}", exchange.getExchangeId() + " - Failed");
            }
            exchange.setException(e);
        }
        callback.done(syncProcessing);
        if(log.isDebugEnabled()) {
            log.debug("Processing Robust-InOnly Exchange id:{}", exchange.getExchangeId() + " - SUCCESS");
        }
        return syncProcessing;
    }
    
    protected boolean processInOut(final Exchange exchange, final AsyncCallback callback) {
        boolean syncProcessing = false;
        if(log.isDebugEnabled()) {
            log.debug("Processing InOut Exchange id:{}", exchange.getExchangeId());
        }
        try {
            if(isSyncronous()) {
                if(log.isDebugEnabled()) {
                    log.debug("  Sending message asynchronously for Exchange id:{}", exchange.getExchangeId());
                }
                executor.submit(new Callable<Object>() {
                    public Object call() throws Exception {
                        sendMessage(exchange);
                        return null;
                    }
                });
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("  Sending message synchronously for Exchange id:{}", exchange.getExchangeId());
                }
                syncProcessing = true;
                sendMessage(exchange);
            }
        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.debug("Processing InOut Exchange id:{}", exchange.getExchangeId() + " - Failed");
            }
            exchange.setException(e);
        }
        // Execute the call back
        callback.done(syncProcessing);
        if(log.isDebugEnabled()) {
            log.debug("Processing InOut Exchange id:{}", exchange.getExchangeId() + " - SUCCESS");
        }
        return syncProcessing;
    }
    
    public abstract void sendMessage(final Exchange exchange) throws Exception;

    
    private MessageProducerModel createProducerModel() throws Exception {
        return doCreateProducerModel();
    }
    
    public abstract MessageProducerModel doCreateProducerModel() throws Exception;

    protected SjmsEndpoint getQueueEndpoint() {
        return (SjmsEndpoint)this.getEndpoint();
    }
    
    protected ConnectionPool getConnectionPool() {
        return getQueueEndpoint().getConnections();
    }
    
    /**
     * Gets the acknowledgment mode for this instance of DestinationProducer.
     *
     * @return the acknowledgment mode
     */
    public int getAcknowledgeMode() {
        return getQueueEndpoint().getAcknowledgementMode();
    }

    /**
     * Gets the boolean value of async for this instance of DestinationProducer.
     *
     * @return true if asynchronous, otherwise it is synchronous 
     */
    public boolean isSyncronous() {
        return getQueueEndpoint().isSynchronous();
    }

    /**
     * Gets the String value of replyTo for this instance of DestinationProducer.
     *
     * @return the replyTo
     */
    public String getReplyTo() {
        return getQueueEndpoint().getNamedReplyTo();
    }

    /**
     * Gets the String value of destinationName for this instance of DestinationProducer.
     *
     * @return the destinationName
     */
    public String getDestinationName() {
        return getQueueEndpoint().getDestinationName();
    }

    /**
     * Gets the int value of maxProducers for this instance of DestinationProducer.
     *
     * @return the maxProducers
     */
    public int getMaxProducers() {
        return getQueueEndpoint().getProducerCount();
    }

    /**
     * Sets the MessageProducerPool value of producers for this instance of SjmsProducer.
     *
     * @param producers Sets MessageProducerPool, default is TODO add default
     */
    public void setProducers(MessageProducerPool producers) {
        this.producers = producers;
    }

    /**
     * Gets the MessageProducerPool value of producers for this instance of SjmsProducer.
     *
     * @return the producers
     */
    public MessageProducerPool getProducers() {
        return producers;
    }
    
    public boolean isEndpointTopic() {
        return getQueueEndpoint().isTopic();
    }
    
    public boolean isEndpointTransacted() {
        return getQueueEndpoint().isTransacted();
    }
    
    /**
     * TransactedQueueProducerSynchronization
     * 
     * TODO is this appropriate for transactions?
     *
     * @author sully6768
     */
    protected class ProducerSynchronization implements Synchronization {
        private MessageProducerModel model;
        
        public ProducerSynchronization(MessageProducerModel model) {
            this.model = model;
        }
        
        /*
         * @see org.apache.camel.spi.Synchronization#onFailure(org.apache.camel.Exchange)
         *
         * @param exchange
         */
        @Override
        public void onFailure(Exchange exchange) {
            if(log.isDebugEnabled()) {
                log.debug("Processing failure of Exchange id:{}", exchange.getExchangeId());
            }
            try {
                if (model != null) {
                    if (model.getSession() != null) {
                        this.model.getSession().rollback();
                    }
                }
            } catch (JMSException e) {
                log.warn("Failed to rollback the session: {}", e.getMessage());
            } finally {
                try {
                    getProducers().returnObject(model);
                } catch (Exception e) {
                    log.warn("Unable to return the producer model to the pool: {}", e.getMessage());
                    exchange.setException(e);
                }
            }
        }
        
        /*
         * @see org.apache.camel.spi.Synchronization#onComplete(org.apache.camel.Exchange)
         *
         * @param exchange
         */
        @Override
        public void onComplete(Exchange exchange) {
            if(log.isDebugEnabled()) {
                log.debug("Processing completion of Exchange id:{}", exchange.getExchangeId());
            }
            try {
                if (model != null) {
                    if (model.getSession() != null) {
                        this.model.getSession().commit();
                    }
                }
            } catch (JMSException e) {
                log.warn("Failed to commit the session: {}", e.getMessage());
                exchange.setException(e);
            } finally {
                try {
                    getProducers().returnObject(model);
                } catch (Exception e) {
                    log.warn("Unable to return the producer model to the pool: {}", e.getMessage());
                    exchange.setException(e);
                }
            }
        }
    }

}
