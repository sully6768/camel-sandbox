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
import java.util.concurrent.TimeoutException;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.camel.AsyncCallback;
import org.apache.camel.CamelException;
import org.apache.camel.Exchange;
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
//    private static final ConcurrentHashMap<String, InOutResponseContainer> responseMap = new ConcurrentHashMap<String, InOutResponseContainer>();
//	private static final Logger ML_LOGGER = LoggerFactory.getLogger(InternalMessageListener.class);
//	private ReadWriteLock lock = new ReentrantReadWriteLock();
    
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
            Connection conn = getConnectionResource().borrowConnection();
            Session session = null;
            if (isEndpointTransacted()) {
                session = conn.createSession(true, Session.SESSION_TRANSACTED);
            } else {
                session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            }
            MessageConsumer messageConsumer = null;
            if (isTopic()) {
                messageConsumer = JmsObjectFactory.createTopicConsumer(session, getNamedReplyTo());
            } else {
                messageConsumer = JmsObjectFactory.createQueueConsumer(session, getNamedReplyTo());
            }
            getConnectionResource().returnConnection(conn);

            messageConsumer.setMessageListener(new MessageListener() {

                @Override
                public void onMessage(Message message) {
                    logger.info("Message Received in the Consumer Pool");
                    logger.info("  Message : {}", message);
                    try {
                        Exchanger<Object> exchanger = exchangerMap.get(message.getJMSCorrelationID());
                        exchanger.exchange(message, getResponseTimeOut(), TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        ObjectHelper.wrapRuntimeCamelException(e);
                    }
                    
                }
            });
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
    
    
    protected class InternalTempDestinationListener implements MessageListener {
    	private final Logger tempLogger = LoggerFactory.getLogger(InternalTempDestinationListener.class);
    	private Exchanger<Object> exchanger;

        /**
		 * TODO Add Constructor Javadoc
		 *
		 * @param exchanger
		 */
		public InternalTempDestinationListener(Exchanger<Object> exchanger) {
			super();
			this.exchanger = exchanger;
		}

		@Override
        public void onMessage(Message message) {
			if(tempLogger.isDebugEnabled()) {
	        	tempLogger.debug("Message Received in the Consumer Pool");
	        	tempLogger.debug("  Message : {}", message);
			}
            try {
                exchanger.exchange(message, getResponseTimeOut(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                ObjectHelper.wrapRuntimeCamelException(e);
            }
            
        }
    }
    
    private MessageConsumerPool consumers;
    boolean useTempDestinations = false;
    
    public InOutProducer(SjmsEndpoint endpoint) {
        super(endpoint);
        endpoint.getConsumerCount();
    }
    
    @Override
    protected void doStart() throws Exception {
    	if (ObjectHelper.isEmpty(getNamedReplyTo())) {
    		if (log.isDebugEnabled()) {
    			log.debug("No reply to destination is defined.  Using temporary destinations.");
    		}
    		useTempDestinations = true;
    	} else {
    		if (log.isDebugEnabled()) {
    			log.debug("Using {} as the reply to destination.", getNamedReplyTo());
    		}
            if (getConsumers() == null) {
                setConsumers(new MessageConsumerPool(getConsumerCount()));
                getConsumers().fillPool();
            }	
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
        Connection conn = getConnectionResource().borrowConnection();
        Session session = null;
        if (isEndpointTransacted()) {
            session = conn.createSession(true, getAcknowledgeMode());
        } else {
            session = conn.createSession(false, getAcknowledgeMode());
        }
        MessageProducer messageProducer = null;
        if(isTopic()) {
            messageProducer = JmsObjectFactory.createMessageProducer(session, getDestinationName(), isTopic(), isPersistent(), getTtl());
        } else {
            messageProducer = JmsObjectFactory.createQueueProducer(session, getDestinationName());
        }
        getConnectionResource().returnConnection(conn);
        return new MessageProducerResources(session, messageProducer);
    }
    
    @Override
    public void sendMessage(final Exchange exchange, final AsyncCallback callback) throws Exception {
        if (getProducers() != null) {
            MessageProducerResources producer = null;
			try {
				producer = getProducers().borrowObject(getResponseTimeOut());
//				producer = getProducers().borrowObject();
			} catch (Exception e1) {
            	log.warn("The producer pool is exhausted.  Consider setting producerCount to a higher value or disable the fixed size of the pool by setting fixedResourcePool=false.");
            	exchange.setException(new Exception("Producer Resource Pool is exhausted"));
			}
            if (producer != null) {

                if (isEndpointTransacted()) {
                    exchange.getUnitOfWork().addSynchronization(new SessionTransactionSynchronization(producer.getSession()));
                }
                
                Message request = JmsMessageHelper.createMessage(exchange, producer.getSession());
                // TODO just set the correlation id don't get it from the message
                String correlationId = null;
                if(exchange.getIn().getHeader("JMSCorrelationID", String.class) == null) {
                    correlationId = UUID.randomUUID().toString().replace("-", "");
                } else {
                    correlationId = exchange.getIn().getHeader("JMSCorrelationID", String.class);
                }
                Object responseObject = null;
                Exchanger<Object> messageExchanger = new Exchanger<Object>();
                JmsMessageHelper.setCorrelationId(request, correlationId);
                if (useTempDestinations) {
                	Destination replyToDestination = JmsObjectFactory.createTemporaryDestination(producer.getSession(), isTopic());
                    MessageConsumer responseConsumer = JmsObjectFactory.createMessageConsumer(producer.getSession(), replyToDestination, null, isTopic(), null, false);
                    responseConsumer.setMessageListener(new InternalTempDestinationListener(messageExchanger));
                    JmsMessageHelper.setJMSReplyTo(request, replyToDestination);
                    producer.getMessageProducer().send(request);
                    
                    try {
						getProducers().returnObject(producer);
					} catch (Exception exception) {
						// thrown if the pool is full. safe to ignore.
					}
					
                    try {
						responseObject = messageExchanger.exchange(null, getResponseTimeOut(), TimeUnit.MILLISECONDS);
					} catch (InterruptedException e) {
						log.debug("Exchanger was interrupted while waiting on response: " + e.getLocalizedMessage(), e);
						exchange.setException(e);
					} catch (TimeoutException e) {
						log.debug("Exchanger timed out while waiting on response: " + e.getLocalizedMessage(), e);
						exchange.setException(e);
					} finally {
						responseConsumer.close();
					}
                } else {
                    exchangerMap.put(request.getJMSCorrelationID(), messageExchanger);
                    Destination replyToDestination = JmsObjectFactory.createDestination(producer.getSession(), getNamedReplyTo(), isTopic());
                    JmsMessageHelper.setJMSReplyTo(request, replyToDestination);
                    
                    producer.getMessageProducer().send(request);
                    try {
						getProducers().returnObject(producer);
					} catch (Exception exception) {
						// thrown if the pool is full. safe to ignore.
					}
                    responseObject = messageExchanger.exchange(null, getResponseTimeOut(), TimeUnit.MILLISECONDS);
                }
                
                if (exchange.getException() == null) {
                    
                    if (responseObject instanceof Throwable) {
                        exchange.setException((Throwable) responseObject);
                    } else if (responseObject instanceof Message) {
                        Message response = (Message) responseObject;
                        JmsMessageHelper.populateExchange(response, exchange, true);
                    } else {
                    	exchange.setException(new CamelException("Unknown response type: " + responseObject));
                    }	
                }
            }
            
            callback.done(isSynchronous());
        }
    }

    public void setConsumers(MessageConsumerPool consumers) {
        this.consumers = consumers;
    }

    public MessageConsumerPool getConsumers() {
        return consumers;
    }
}
