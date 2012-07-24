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
package org.apache.camel.component.sjms.producer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.sjms.SjmsEndpoint;
import org.apache.camel.component.sjms.SjmsProducer;
import org.apache.camel.component.sjms.jms.JmsMessageHelper;
import org.apache.camel.component.sjms.jms.JmsObjectFactory;
import org.apache.camel.component.sjms.pool.ObjectPool;
import org.apache.camel.component.sjms.tx.SessionTransactionSynchronization;
import org.apache.camel.util.ObjectHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Add Class documentation for InOutProducer
 *
 */
public class InOutProducer extends SjmsProducer {
    
    private static ConcurrentHashMap<String, Exchanger<Object>> exchangerMap = new ConcurrentHashMap<String, Exchanger<Object>>();
    private static final ConcurrentHashMap<String, InOutResponseContainer> responseMap = new ConcurrentHashMap<String, InOutResponseContainer>();
	private static final Logger ML_LOGGER = LoggerFactory.getLogger(InternalMessageListener.class);
	private ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * TODO Add Class documentation for MessageProducerPool
     *
     * @author sully6768
     */
    protected class MessageConsumerPool extends ObjectPool<MessageConsumerResource>{

        /**
         * TODO Add Constructor Javadoc
         *
         * @param poolSize
         */
        public MessageConsumerPool(int poolSize) {
            super(poolSize);
        }

        @Override
        protected MessageConsumerResource createObject() throws Exception {
            Connection conn = getConnectionPool().borrowConnection(5000);
            Session session = conn.createSession(false, getAcknowledgeMode());
            MessageConsumer messageConsumer = JmsObjectFactory.createQueueConsumer(session, getNamedReplyTo());

            messageConsumer.setMessageListener(new MessageListener() {

                @Override
                public void onMessage(Message message) {
                    logger.info("Message Received in the Consumer Pool");
                    logger.info("  Message : {}", message);
                    try {
                        Exchanger<Object> exchanger = exchangerMap.get(message.getJMSCorrelationID());
                        exchanger.exchange(message, timeout, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        ObjectHelper.wrapRuntimeCamelException(e);
                    }
                    
                }
            });
//            if(! isSynchronous()) {
//            	messageConsumer.setMessageListener(new InternalMessageListener());
//            }
            getConnectionPool().returnConnection(conn);
            MessageConsumerResource mcm = new MessageConsumerResource(session, messageConsumer);
            return mcm;
        }
        
        @Override
        protected void destroyObject(MessageConsumerResource model) throws Exception {
            if (model.getMessageConsumer() != null) {
                model.getMessageConsumer().close();
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
     * TODO Add Class documentation for MessageConsumerResource
     */
    protected class MessageConsumerResource {
        private final Session session;
        private final MessageConsumer messageConsumer;

        /**
         * TODO Add Constructor Javadoc
         * 
         * @param session
         * @param messageConsumer
         */
        public MessageConsumerResource(Session session, MessageConsumer messageConsumer) {
            super();
            this.session = session;
            this.messageConsumer = messageConsumer;
        }

        /**
         * Gets the Session value of session for this instance of
         * MessageConsumerResource.
         * 
         * @return the session
         */
        public Session getSession() {
            return session;
        }

        /**
         * Gets the MessageConsumer value of queueSender for this instance of
         * MessageConsumerResource.
         * 
         * @return the queueSender
         */
        public MessageConsumer getMessageConsumer() {
            return messageConsumer;
        }
    }

    protected class InOutResponseContainer {
        private final Exchange exchange;
        private final AsyncCallback callback;

        /**
         * 
         * @param exchange
         * @param callback
         */
        public InOutResponseContainer(Exchange exchange, AsyncCallback callback) {
            super();
            this.exchange = exchange;
            this.callback = callback;
        }

        public Exchange getExchange() {
            return exchange;
        }

        public AsyncCallback getCallback() {
            return callback;
        }
    }
    
    private class InternalMessageListener implements MessageListener {

        @Override
        public void onMessage(Message message) {
        	ML_LOGGER.info("Message Received in the Consumer Pool");
        	ML_LOGGER.info("  Message : {}", message);
            try {
//                Exchanger<Object> exchanger = exchangerMap.get(message.getJMSCorrelationID());
                InOutResponseContainer iorc = null;
                if (responseMap.containsKey(message.getJMSCorrelationID())) {
                    iorc = responseMap.get(message.getJMSCorrelationID());
                    Exchange exchange = iorc.getExchange();
                    if (message instanceof Throwable) {
                        exchange.setException((Throwable) message);
                    } else if (message instanceof Message) {
                        Message response = (Message) message;
                        JmsMessageHelper.populateExchange(response, exchange, true);
                    } else {
                        throw new RuntimeCamelException( "Unknown response type: " + message);
                    }
                    iorc.getCallback().done(isSynchronous());
                } else {
                    
                }
//                exchanger.exchange(message, timeout, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                ObjectHelper.wrapRuntimeCamelException(e);
            }
            
        }
    }
    private MessageConsumerPool consumers;
    private long timeout = 300000;
    
    public InOutProducer(SjmsEndpoint endpoint) {
        super(endpoint);
        endpoint.getConsumerCount();
    }
    
    @Override
    protected void doStart() throws Exception {
        if (getConsumers() == null) {
            setConsumers(new MessageConsumerPool(getConsumerCount()));
            getConsumers().fillPool();
        }
        super.doStart();
    }
    
    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (getConsumers() != null) {
            getConsumers().drainPool();
            setConsumers(null);
        }
    }
    
    public MessageProducerResources doCreateProducerModel() throws Exception {
        Connection conn = getConnectionPool().borrowConnection();
        Session session = conn.createSession(false, getAcknowledgeMode());
        MessageProducer messageProducer = null;
        messageProducer = JmsObjectFactory.createQueueProducer(session, getDestinationName());
        getConnectionPool().returnConnection(conn);
        return new MessageProducerResources(session, messageProducer);
    }
    
    public void sendMessage(final Exchange exchange, final AsyncCallback callback) throws Exception {
        if (getProducers() != null) {
            final MessageProducerResources producer = getProducers().borrowObject();

            if (isEndpointTransacted()) {
                exchange.getUnitOfWork().addSynchronization(new SessionTransactionSynchronization(producer.getSession()));
            }
            
            Message request = JmsMessageHelper.createMessage(exchange, producer.getSession());
            Exchanger<Object> messageExchanger = new Exchanger<Object>();
            String correlationId = null;
            if(exchange.getIn().getHeader("JMSCorrelationID", String.class) == null) {
                correlationId = UUID.randomUUID().toString().replace("-", "");
            } else {
                correlationId = exchange.getIn().getHeader("JMSCorrelationID", String.class);
            }
            
            JmsMessageHelper.setCorrelationId(request, correlationId);
            exchangerMap.put(request.getJMSCorrelationID(), messageExchanger);
            Destination replyToDestination = JmsObjectFactory.createQueue(producer.getSession(), getNamedReplyTo());
            JmsMessageHelper.setJMSReplyTo(request, replyToDestination);
            producer.getMessageProducer().send(request);
            
            Object responseObject = messageExchanger.exchange(null, timeout, TimeUnit.MILLISECONDS);
            getProducers().returnObject(producer);
            
            if (responseObject instanceof Throwable) {
                exchange.setException((Throwable) responseObject);
            } else if (responseObject instanceof Message) {
                Message response = (Message) responseObject;
                JmsMessageHelper.populateExchange(response, exchange, true);
            } else {
                throw new RuntimeCamelException("Unknown response type: " + responseObject);
            }
            
            callback.done(isSynchronous());
            
            
//            final MessageProducerResources producer = getProducers().borrowObject(5000);
//
//            if (isEndpointTransacted()) {
//                exchange.getUnitOfWork().addSynchronization(new SessionTransactionSynchronization(producer.getSession()));
//            }
//            
//            Message request = JmsMessageHelper.createMessage(exchange, producer.getSession());
//            String correlationId = null;
//            if(exchange.getIn().getHeader(JmsMessageHeaderType.JMSCorrelationID.toString(), String.class) == null) {
//                correlationId = UUID.randomUUID().toString().replace("-", "");
//            } else {
//                correlationId = exchange.getIn().getHeader(JmsMessageHeaderType.JMSCorrelationID.toString(), String.class);
//            }
//
//            
//            JmsMessageHelper.setCorrelationId(request, correlationId);
//            exchangerMap.put(request.getJMSCorrelationID(), messageExchanger);
//            Destination replyToDestination = JmsObjectFactory.createQueue(producer.getSession(), getNamedReplyTo());
//            JmsMessageHelper.setJMSReplyTo(request, replyToDestination);
//            producer.getMessageProducer().send(request);
//            
//            Object responseObject = messageExchanger.exchange(null, timeout, TimeUnit.MILLISECONDS);
//            getProducers().returnObject(producer);
//            
//            if (responseObject instanceof Throwable) {
//                exchange.setException((Throwable) responseObject);
//            } else if (responseObject instanceof Message) {
//                Message response = (Message) responseObject;
//                JmsMessageHelper.populateExchange(response, exchange, true);
//            } else {
//                throw new RuntimeCamelException("Unknown response type: " + responseObject);
//            }
//            
//            JmsMessageHelper.setCorrelationId(request, correlationId);
//            responseMap.put(correlationId, new InOutResponseContainer(exchange, callback));
//            Destination replyToDestination = JmsObjectFactory.createQueue(producer.getSession(), getNamedReplyTo());
//            JmsMessageHelper.setJMSReplyTo(request, replyToDestination);
//            producer.getMessageProducer().send(request);
//            getProducers().returnObject(producer);
//            
//            if (isSynchronous()) {
//                if (getConsumers() != null) {
//                	MessageConsumerResource consumer = getConsumers().borrowObject(timeout);
//                	Message message = consumer.getMessageConsumer().receive(timeout);
//                    if (message instanceof Throwable) {
//                        exchange.setException((Throwable) message);
//                    } else if (message instanceof Message) {
//                        Message response = (Message) message;
//                        JmsMessageHelper.populateExchange(response, exchange, true);
//                    } else {
//                        throw new RuntimeCamelException( "Unknown response type: " + message);
//                    }
//                    getConsumers().returnObject(consumer);
//                    callback.done(isSynchronous());
//                }
//            }
        }
    }

    /**
     * Sets the MessageConsumerPool value of consumers for this instance of InOutProducer.
     *
     * @param consumers Sets MessageConsumerPool, default is TODO add default
     */
    public void setConsumers(MessageConsumerPool consumers) {
        this.consumers = consumers;
    }

    /**
     * Gets the MessageConsumerPool value of consumers for this instance of InOutProducer.
     *
     * @return the consumers
     */
    public MessageConsumerPool getConsumers() {
        return consumers;
    }
}
