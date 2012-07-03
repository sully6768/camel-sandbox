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

import java.util.concurrent.ExecutorService;

import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.component.sjms.pool.ConnectionPool;
import org.apache.camel.component.sjms.pool.ObjectPool;
import org.apache.camel.impl.DefaultAsyncProducer;
import org.apache.camel.util.ObjectHelper;

/**
 * TODO Add Class documentation for SjmsProducer
 *
 * @author sully6768
 */
public abstract class SjmsProducer extends DefaultAsyncProducer  {

    
    /**
     * TODO Add Class documentation for MessageProducerPool
     *
     * @author sully6768
     */
    protected class MessageProducerPool extends ObjectPool<MessageProducerModel>{

        /**
         * TODO Add Constructor Javadoc
         */
        public MessageProducerPool() {
            super(getProducerCount());
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
    
    /**
     * TODO Add Class documentation for MessageProducerModel
     */
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
        this.executor = endpoint.getCamelContext().getExecutorServiceManager().newDefaultThreadPool(this, "SjmsProducer");
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
    
    private MessageProducerModel createProducerModel() throws Exception {
        return doCreateProducerModel();
    }
    
    public abstract MessageProducerModel doCreateProducerModel() throws Exception;
    
    public abstract void sendMessage(Exchange exchange) throws Exception;
    
    @Override
    public boolean process(final Exchange exchange, final AsyncCallback callback) {
        if(log.isDebugEnabled()) {
            log.debug("Processing Exchange.id:{}", exchange.getExchangeId());
        }
        try {
            if( ! isSynchronous()) {
                if(log.isDebugEnabled()) {
                    log.debug("  Sending message asynchronously for Exchange id:{}", exchange.getExchangeId());
                }
                getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            sendMessage(exchange);
                            // Execute the call back
                            callback.done(isSynchronous());
                        } catch (Exception e) {
                            ObjectHelper.wrapRuntimeCamelException(e);
                        }
                        
                    }
                });
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("  Sending message synchronously for Exchange id:{}", exchange.getExchangeId());
                }
                sendMessage(exchange);
                callback.done(isSynchronous());
            }
        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.debug("Processing Exchange.id:{}", exchange.getExchangeId() + " - FAILED");
            }
            exchange.setException(e);
        }
        if(log.isDebugEnabled()) {
            log.debug("Processing Exchange.id:{}", exchange.getExchangeId() + " - SUCCESS");
        }
        return isSynchronous();
    }

    protected SjmsEndpoint getSjmsEndpoint() {
        return (SjmsEndpoint)this.getEndpoint();
    }
    
    protected ConnectionPool getConnectionPool() {
        return getSjmsEndpoint().getConnections();
    }
    
    /**
     * Gets the acknowledgment mode for this instance of DestinationProducer.
     *
     * @return the acknowledgment mode
     */
    public int getAcknowledgeMode() {
        return getSjmsEndpoint().getAcknowledgementMode().intValue();
    }

    /**
     * Gets the boolean value of async for this instance of DestinationProducer.
     *
     * @return true if asynchronous, otherwise it is synchronous 
     */
    public boolean isSynchronous() {
        return getSjmsEndpoint().isSynchronous();
    }

    /**
     * Gets the String value of replyTo for this instance of DestinationProducer.
     *
     * @return the replyTo
     */
    public String getReplyTo() {
        return getSjmsEndpoint().getNamedReplyTo();
    }

    /**
     * Gets the String value of destinationName for this instance of DestinationProducer.
     *
     * @return the destinationName
     */
    public String getDestinationName() {
        return getSjmsEndpoint().getDestinationName();
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

    /**
     * Test to verify if this endpoint is a JMS Topic or Queue.
     * 
     * @return true if it is a Topic, otherwise it is a Queue
     */
    public boolean isTopic() {
        return getSjmsEndpoint().isTopic();
    }

    /**
     * Test to determine if this endpoint should use a JMS Transaction.
     * 
     * @return true if it is a Topic, otherwise it is a Queue
     */
    public boolean isEndpointTransacted() {
        return getSjmsEndpoint().isTransacted();
    }

    /**
     * Returns the named reply to value for this producer
     * 
     * @return true if it is a Topic, otherwise it is a Queue
     */
    public String getNamedReplyTo() {
        return getSjmsEndpoint().getNamedReplyTo();
    }

    /**
     * Gets the int value of producerCount for this instance of SjmsProducer.
     *
     * @return the producerCount
     */
    public int getProducerCount() {
        return getSjmsEndpoint().getProducerCount();
    }

    /**
     * Gets the int value of consumerCount for this instance of SjmsProducer.
     *
     * @return the consumerCount
     */
    public int getConsumerCount() {
        return getSjmsEndpoint().getConsumerCount();
    }

    /**
     * Gets the ExecutorService value of executor for this instance of SjmsProducer.
     *
     * @return the executor
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Gets the long value of ttl for this instance of SjmsProducer.
     *
     * @return the ttl
     */
    public long getTtl() {
        return  getSjmsEndpoint().getTtl();
    }

    /**
     * Gets the boolean value of persistent for this instance of SjmsProducer.
     *
     * @return the persistent
     */
    public boolean isPersistent() {
        return  getSjmsEndpoint().isPersistent();
    }

}
