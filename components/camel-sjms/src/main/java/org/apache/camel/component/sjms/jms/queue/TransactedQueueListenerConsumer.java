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
package org.apache.camel.component.sjms.jms.queue;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.camel.Processor;
import org.apache.camel.component.sjms.MessageHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Add Class documentation for QueueListenerConsumer
 *
 */
public class TransactedQueueListenerConsumer extends QueueListenerConsumer {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public TransactedQueueListenerConsumer(QueueEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        for (int i = 0; i < getMaxConsumers(); i++) {
            
            Connection conn = getConnectionPool().borrowObject();
            QueueSession queueSession = (QueueSession) conn.createSession(true, Session.AUTO_ACKNOWLEDGE);
            QueueReceiver messageConsumer = createConsumer(queueSession);
            MessageHandler handler = createMessageHandler(queueSession);
            getConnectionPool().returnObject(conn);
            
            lock.writeLock().lock();
            try {
                String id = createId();
                getMessageHandlers().put(id, handler);
                messageConsumer.setMessageListener(handler);
                getMessageConsumers().put(id, messageConsumer);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * @param queueSession
     * @return
     * @throws JMSException
     */
    protected QueueReceiver createQueueReceiver(QueueSession queueSession)
            throws JMSException {
        QueueReceiver messageConsumer;
        Queue myQueue = queueSession.createQueue(getDestinationName());
        messageConsumer = queueSession.createReceiver(myQueue);
        return messageConsumer;
    }
}
