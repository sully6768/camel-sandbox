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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.Connection;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.component.sjms.SjmsEndpoint;
import org.apache.camel.component.sjms.jms.JmsObjectFactory;
import org.apache.camel.component.sjms.pool.ObjectPool;
import org.apache.camel.component.sjms.tx.SessionTransactionSynchronization;

/**
 * A non-transacted queue consumer for a given JMS Destination
 *
 */
public class QueueListenerConsumer extends QueueConsumer {

    private AtomicBoolean stopped = new AtomicBoolean(false);
    protected MessageConsumerPool consumers;
    private final ExecutorService executor;
    
    
    protected class MessageConsumerPool extends ObjectPool<MessageConsumerModel>{

        /**
         * TODO Add Constructor Javadoc
         *
         * @param sessionPool
         */
        public MessageConsumerPool() {
            super(getConsumerCount());
        }

        @Override
        protected MessageConsumerModel createObject() throws Exception {
            MessageConsumerModel model = null;
            if (isEndpointTransacted() || getSjmsEndpoint().getExchangePattern().equals(ExchangePattern.InOut)) {
                model = createConsumerWithDedicatedSession();
            } else {
                model = createConsumerListener();
            }
            return model;
        }
        
        @Override
        protected void destroyObject(MessageConsumerModel model) throws Exception {
            if (model != null) {
                if(model.getMessageConsumer() != null) {
                    if(model.getMessageConsumer().getMessageListener() != null) {
                        model.getMessageConsumer().setMessageListener(null);
                    }
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
    }
    
    protected class MessageConsumerModel {
        private final Session session;
        private final MessageConsumer messageConsumer;

        /**
         * TODO Add Constructor Javadoc
         * 
         * @param session
         * @param messageProducer
         */
        public MessageConsumerModel(MessageConsumer messageConsumer) {
            super();
            this.session = null;
            this.messageConsumer = messageConsumer;
        }

        /**
         * TODO Add Constructor Javadoc
         * 
         * @param session
         * @param messageProducer
         */
        public MessageConsumerModel(Session session, MessageConsumer messageConsumer) {
            super();
            this.session = session;
            this.messageConsumer = messageConsumer;
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
        public MessageConsumer getMessageConsumer() {
            return messageConsumer;
        }
    }
    
    public QueueListenerConsumer(SjmsEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.executor = endpoint.getCamelContext().getExecutorServiceManager().newDefaultThreadPool(this, "SjmsConsumer");
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        consumers = new MessageConsumerPool();
        consumers.fillPool();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (consumers != null) {
            consumers.drainPool();
            consumers = null;
        }
    }
    
    @Override
    protected void doResume() throws Exception {
        super.doResume();
        getStopped().set(false);
    }
    
    @Override
    protected void doSuspend() throws Exception {
        getStopped().set(true);
        super.doSuspend();
    }
    
    private MessageConsumerModel createConsumerWithDedicatedSession() throws Exception {
        Connection conn = getConnectionPool().borrowObject();
        Session session = null;
        if (isEndpointTransacted()) {
            session = conn.createSession(true, Session.SESSION_TRANSACTED);
        } else {
            session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        }
        MessageConsumer messageConsumer = null;
        if (isTopic()) {
            messageConsumer = JmsObjectFactory.createTopicConsumer(session, getDestinationName());    
        } else {
            messageConsumer = JmsObjectFactory.createQueueConsumer(session, getDestinationName());
        }
        MessageListener handler = createMessageHandler(session);
        messageConsumer.setMessageListener(handler);
        getConnectionPool().returnObject(conn);
        return new MessageConsumerModel(session, messageConsumer);
    }
    
    private MessageConsumerModel createConsumerListener() throws Exception {
        Session queueSession = getSessionPool().borrowObject();
        MessageConsumer messageConsumer = null;
        if (isTopic()) {
            messageConsumer = JmsObjectFactory.createTopicConsumer(queueSession, getDestinationName());    
        } else {
            messageConsumer = JmsObjectFactory.createQueueConsumer(queueSession, getDestinationName());
        }
        getSessionPool().returnObject(queueSession);
        // Don't pass in the session.  Only needed if we are transacted
        MessageListener handler = createMessageHandler(null);
        messageConsumer.setMessageListener(handler);
        return new MessageConsumerModel(messageConsumer);
    } 

    /**
     * Helper factory method used to create a SjmsMessageConsumer based on the MEP
     * 
     * @param session
     *            a session is only required if we are a transacted consumer
     * @return
     */
    protected MessageListener createMessageHandler(Session session) {
        MessageListener answer = null;
        if (getSjmsEndpoint().getExchangePattern().equals(ExchangePattern.InOnly)){
            DefaultMessageHandler messageHandler = null;
            if (isEndpointTransacted()) {
                messageHandler = new InOnlyMessageHandler(getEndpoint(), getStopped(), executor, new SessionTransactionSynchronization(session));
            } else {
                messageHandler = new InOnlyMessageHandler(getEndpoint(), getStopped(), executor);
            }
            messageHandler.setSession(session);
            messageHandler.setProcessor(getAsyncProcessor());
            messageHandler.setSynchronous(isSynchronous());
            messageHandler.setTransacted(isEndpointTransacted());
            answer = messageHandler;
        } else {
            DefaultMessageHandler messageHandler = null;
            if (isEndpointTransacted()) {
                messageHandler = new InOutMessageHandler(getEndpoint(), getStopped(), executor, new SessionTransactionSynchronization(session));
            } else {
                messageHandler = new InOutMessageHandler(getEndpoint(), getStopped(), executor);
            }
            messageHandler.setSession(session);
            messageHandler.setProcessor(getAsyncProcessor());
            messageHandler.setSynchronous(isSynchronous());
            messageHandler.setTransacted(isEndpointTransacted());
            answer = messageHandler;
        }
        return answer;
    }

    /**
     * Sets the AtomicBoolean value of stopped for this instance of
     * QueueListenerConsumer.
     * 
     * @param stopped
     *            Sets AtomicBoolean, default is TODO add default
     */
    protected void setStopped(boolean flag) {
        this.stopped.set(flag);
    }

    /**
     * Gets the AtomicBoolean value of stopped for this instance of
     * QueueListenerConsumer.
     * 
     * @return the stopped
     */
    protected AtomicBoolean getStopped() {
        return stopped;
    }
}
