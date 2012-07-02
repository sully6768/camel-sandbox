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

import java.util.concurrent.Exchanger;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.apache.camel.component.sjms.SjmsMessageConsumer;
import org.apache.camel.component.sjms.jms.JmsObjectFactory;
import org.apache.camel.component.sjms.pool.ConnectionPool;
import org.apache.camel.component.sjms.pool.SessionPool;

/**
 * TODO Add Class documentation for InOutMessageProducer
 *
 * @author sully6768
 */
public class InOutMessageConsumer implements SjmsMessageConsumer {
    private Session session;
    private MessageConsumer messageConsumer;
    private Exchanger<Object> exchanger;

    /**
     * TODO Add Constructor Javadoc
     * 
     * @param session
     * @param messageProducer
     */
    public InOutMessageConsumer() {
    }

    /**
     * TODO Add Constructor Javadoc
     * 
     * @param session
     * @param messageProducer
     */
    private InOutMessageConsumer(Session session, MessageConsumer messageConsumer) {
        this.session = session;
        this.messageConsumer = messageConsumer;
    }

    /**
     * TODO Add Constructor Javadoc
     * 
     * @param session
     * @param messageProducer
     */
    private InOutMessageConsumer(Session session, MessageConsumer messageConsumer, Exchanger<Object> exchanger) {
        this.session = session;
        this.messageConsumer = messageConsumer;
        this.exchanger = exchanger;
    }

    @Override
    public void onMessage(Message message) {
        
    }

    @Override
    public void handleMessage(Message message) {
        
    }

    @Override
    public InOutMessageConsumer createMessageConsumer(
            ConnectionPool connectionPool, String destinationName)
            throws Exception {
        Connection conn = connectionPool.borrowObject();
        session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        messageConsumer = JmsObjectFactory.createQueueConsumer(session, destinationName);
        connectionPool.returnObject(conn);
        return new InOutMessageConsumer(session, messageConsumer);
    }

    @Override
    public InOutMessageConsumer createMessageConsumerListener(
            SessionPool sessionPool, String destinationName, Exchanger<Object> exchanger) throws Exception {
        session = sessionPool.borrowObject();
        messageConsumer = JmsObjectFactory.createQueueConsumer(session, destinationName);
        sessionPool.returnObject(session);
        return new InOutMessageConsumer(null, messageConsumer, exchanger);
    }

    @Override
    public void destroyMessageConsumer() throws Exception {
        if (getMessageConsumer() != null) {
            getMessageConsumer().close();
        }
        
        if(getSession() != null) {
            if (getSession().getTransacted()) {
                try {
                    getSession().rollback();
                } catch (Exception e) {
                    // Do nothing.  Just make sure we are cleaned up
                }
            }
            getSession().close();
        }
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
