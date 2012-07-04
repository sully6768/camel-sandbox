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
package org.apache.camel.component.sjms.producer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.sjms.JmsMessageHelper;
import org.apache.camel.component.sjms.SjmsEndpoint;
import org.apache.camel.component.sjms.SjmsProducer;
import org.apache.camel.component.sjms.jms.JmsObjectFactory;
import org.apache.camel.component.sjms.pool.ObjectPool;
import org.apache.camel.component.sjms.tx.SessionTransactionSynchronization;
import org.apache.camel.util.ObjectHelper;

/**
 * TODO Add Class documentation for InOutProducer
 *
 */
public class InOutProducer extends SjmsProducer {
    
    private static ConcurrentHashMap<String, Exchanger<Object>> exchangerMap = new ConcurrentHashMap<String, Exchanger<Object>>();
    
    /**
     * TODO Add Class documentation for MessageProducerPool
     *
     * @author sully6768
     */
    protected class MessageConsumerPool extends ObjectPool<MessageConsumerContainer>{

        /**
         * TODO Add Constructor Javadoc
         *
         * @param poolSize
         */
        public MessageConsumerPool(int poolSize) {
            super(poolSize);
        }

        @Override
        protected MessageConsumerContainer createObject() throws Exception {
            Connection conn = getConnectionPool().borrowObject();
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
            getConnectionPool().returnObject(conn);
            MessageConsumerContainer mcm = new MessageConsumerContainer(session, messageConsumer);
            return mcm;
        }
        
        @Override
        protected void destroyObject(MessageConsumerContainer model) throws Exception {
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
     * TODO Add Class documentation for MessageProducerContainer
     */
    protected class MessageConsumerContainer {
        private final Session session;
        private final MessageConsumer messageConsumer;

        /**
         * TODO Add Constructor Javadoc
         * 
         * @param session
         * @param messageConsumer
         */
        public MessageConsumerContainer(Session session, MessageConsumer messageConsumer) {
            super();
            this.session = session;
            this.messageConsumer = messageConsumer;
        }

        /**
         * Gets the Session value of session for this instance of
         * MessageConsumerContainer.
         * 
         * @return the session
         */
        public Session getSession() {
            return session;
        }

        /**
         * Gets the MessageConsumer value of queueSender for this instance of
         * MessageConsumerContainer.
         * 
         * @return the queueSender
         */
        public MessageConsumer getMessageConsumer() {
            return messageConsumer;
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
    
    public MessageProducerContainer doCreateProducerModel() throws Exception {
        Connection conn = getConnectionPool().borrowObject();
        Session session = conn.createSession(false, getAcknowledgeMode());
        MessageProducer messageProducer = null;
        messageProducer = JmsObjectFactory.createQueueProducer(session, getDestinationName());
        getConnectionPool().returnObject(conn);
        return new MessageProducerContainer(session, messageProducer);
    }
    
    public void sendMessage(final Exchange exchange) throws Exception {
        if (getProducers() != null) {
            final MessageProducerContainer producer = getProducers().borrowObject();

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
