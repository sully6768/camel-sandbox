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

import javax.jms.Connection;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.camel.Processor;
import org.apache.camel.component.sjms.SjmsMessageConsumer;
import org.apache.camel.component.sjms.SjmsEndpoint;

/**
 * TODO Add Class documentation for TransactedQueueListenerConsumer
 *
 */
public class TransactedQueueListenerConsumer extends QueueListenerConsumer {

    public TransactedQueueListenerConsumer(SjmsEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }
    
    protected MessageConsumerModel doCreateConsumer() throws Exception {
        Connection conn = getConnectionPool().borrowObject();
        QueueSession queueSession = (QueueSession) conn.createSession(true, Session.AUTO_ACKNOWLEDGE);
        Queue myQueue = queueSession.createQueue(getDestinationName());
        MessageConsumer messageConsumer = queueSession.createReceiver(myQueue);
        SjmsMessageConsumer handler = createMessageHandler(queueSession);
        messageConsumer.setMessageListener(handler);
        getConnectionPool().returnObject(conn);
        return new MessageConsumerModel(queueSession, messageConsumer);
    }
    
//    /**
//     * TransactedQueueProducerSynchronization
//     * 
//     * TODO is this appropriate for transactions?
//     *
//     * @author sully6768
//     */
//    class TransactedQueueConsumerSynchronization implements Synchronization {
//        private MessageProducerModel model;
//        
//        public TransactedQueueConsumerSynchronization(MessageProducerModel model) {
//            this.model = model;
//        }
//        
//        /*
//         * @see org.apache.camel.spi.Synchronization#onFailure(org.apache.camel.Exchange)
//         *
//         * @param exchange
//         */
//        @Override
//        public void onFailure(Exchange exchange) {
//            if(log.isDebugEnabled()) {
//                log.debug("Processing failure of Exchange id:{}", exchange.getExchangeId());
//            }
//            try {
//                if (model != null) {
//                    if (model.getSession() != null) {
//                        this.model.getSession().rollback();
//                    }
//                }
//            } catch (JMSException e) {
//                log.warn("Failed to rollback the session: {}", e.getMessage());
//            } finally {
//                try {
//                    producers.returnObject(model);
//                } catch (Exception e) {
//                    log.warn("Unable to return the producer model to the pool: {}", e.getMessage());
//                    exchange.setException(e);
//                }
//            }
//        }
//        
//        /*
//         * @see org.apache.camel.spi.Synchronization#onComplete(org.apache.camel.Exchange)
//         *
//         * @param exchange
//         */
//        @Override
//        public void onComplete(Exchange exchange) {
//            if(log.isDebugEnabled()) {
//                log.debug("Processing completion of Exchange id:{}", exchange.getExchangeId());
//            }
//            try {
//                if (model != null) {
//                    if (model.getSession() != null) {
//                        this.model.getSession().commit();
//                    }
//                }
//            } catch (JMSException e) {
//                log.warn("Failed to commit the session: {}", e.getMessage());
//                exchange.setException(e);
//            } finally {
//                try {
//                    producers.returnObject(model);
//                } catch (Exception e) {
//                    log.warn("Unable to return the producer model to the pool: {}", e.getMessage());
//                    exchange.setException(e);
//                }
//            }
//        }
//    }
}
