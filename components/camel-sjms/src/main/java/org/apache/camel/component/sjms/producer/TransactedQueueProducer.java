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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.camel.Exchange;
import org.apache.camel.component.sjms.JmsMessageHelper;
import org.apache.camel.component.sjms.SjmsEndpoint;
import org.apache.camel.spi.Synchronization;

/**
 * TransactedQueueProducer
 *
 */
public class TransactedQueueProducer extends QueueProducer {
    
    public TransactedQueueProducer(SjmsEndpoint endpoint) {
        super(endpoint);
    }
    
    /*
     * @see org.apache.camel.component.sjms.jms.queue.QueueProducer#doCreateProducerModel()
     *
     * @return
     * @throws Exception
     */
    @Override
    public MessageProducerModel doCreateProducerModel() throws Exception {
        QueueConnection conn = (QueueConnection) getConnectionPool().borrowObject();
        QueueSession queueSession = (QueueSession) conn.createSession(true, Session.SESSION_TRANSACTED);
        Queue myQueue = queueSession.createQueue(getDestinationName());
        QueueSender queueSender = queueSession.createSender(myQueue);
        getConnectionPool().returnObject(conn);
        return new MessageProducerModel(queueSession, queueSender);
    }
    

    /*
     * @see org.apache.camel.component.sjms.SjmsProducer#sendMessage(org.apache.camel.Exchange)
     *
     * @param exchange
     * @throws Exception
     */
    @Override
    protected void sendMessage(Exchange exchange) throws Exception {
        if (producers != null) {
            MessageProducerModel model = producers.borrowObject();
            exchange.addOnCompletion(new TransactedQueueProducerSynchronization(model));
            Message message = JmsMessageHelper.createMessage(exchange, model.getSession());
            model.getMessageProducer().send(message);
        }
    }
    
    /**
     * TransactedQueueProducerSynchronization
     * 
     * TODO is this appropriate for transactions?
     *
     * @author sully6768
     */
    class TransactedQueueProducerSynchronization implements Synchronization {
        private MessageProducerModel model;
        
        public TransactedQueueProducerSynchronization(MessageProducerModel model) {
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
                    producers.returnObject(model);
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
                    producers.returnObject(model);
                } catch (Exception e) {
                    log.warn("Unable to return the producer model to the pool: {}", e.getMessage());
                    exchange.setException(e);
                }
            }
        }
    }
}
