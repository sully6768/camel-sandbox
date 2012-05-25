/*
 * Copyright 2012 FuseSource
 *
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
package org.apache.camel.component.sjms.jms.queue;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.camel.Exchange;
import org.apache.camel.component.sjms.pool.ObjectPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Add Class documentation for QueueConsumer
 *
 */
public class TransactedQueueProducer extends QueueProducer {
    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    private TransactedQueueProducerPool transactedProducers;
    private Session messageSession = null;
    
    private class TransactedQueueProducerPool extends ObjectPool<SessionProducerPair>{

        /**
         * TODO Add Constructor Javadoc
         *
         * @param sessionPool
         */
        public TransactedQueueProducerPool() {
            super(getMaxProducers());
        }

        @Override
        protected SessionProducerPair createObject() throws Exception {
            QueueConnection conn = (QueueConnection) getConnectionPool().borrowObject();
            QueueSession queueSession = (QueueSession) conn.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Queue myQueue = queueSession.createQueue(getDestinationName());
            QueueSender queueSender = queueSession.createSender(myQueue);
            getConnectionPool().returnObject(conn);
            return new SessionProducerPair(queueSession, queueSender);
        }
        
        @Override
        protected void destroyObject(SessionProducerPair pair) throws Exception {
            if (pair != null) {
                pair.getQueueSender().close();
                pair.getSession().close();
            }
        }
    }
    
    private class SessionProducerPair {
        private final Session session;
        private final QueueSender queueSender;

        /**
         * TODO Add Constructor Javadoc
         * 
         * @param session
         * @param queueSender
         */
        private SessionProducerPair(Session session, QueueSender queueSender) {
            super();
            this.session = session;
            this.queueSender = queueSender;
        }

        /**
         * Gets the Session value of session for this instance of
         * SessionProducerPair.
         * 
         * @return the session
         */
        public Session getSession() {
            return session;
        }

        /**
         * Gets the QueueSender value of queueSender for this instance of
         * SessionProducerPair.
         * 
         * @return the queueSender
         */
        public QueueSender getQueueSender() {
            return queueSender;
        }
    }
    
    public TransactedQueueProducer(QueueEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        transactedProducers = new TransactedQueueProducerPool();
        transactedProducers.fillPool();
        Connection conn = getConnectionPool().borrowObject();
        messageSession = conn.createSession(true, Session.AUTO_ACKNOWLEDGE);
        getConnectionPool().returnObject(conn);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (transactedProducers != null) {
            transactedProducers.drainPool();
            transactedProducers = null;
            messageSession.close();
            messageSession = null;
        }
    }

    
    protected void sendMessage(Exchange exchange) throws Exception {
        SessionProducerPair pair = transactedProducers.borrowObject();
        Message message = createMessage(exchange, pair.getSession());
        try {
            pair.getQueueSender().send(message);
            pair.getSession().commit();
        } catch (Exception e) {
            // e.printStackTrace();
            log.error("TODO Auto-generated catch block", e);
        }
        transactedProducers.returnObject(pair);
    }

}
