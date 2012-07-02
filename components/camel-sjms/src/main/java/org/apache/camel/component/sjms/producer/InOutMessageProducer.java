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

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.camel.component.sjms.SjmsMessageProducer;
import org.apache.camel.component.sjms.jms.JmsObjectFactory;
import org.apache.camel.component.sjms.pool.ConnectionPool;

/**
 * TODO Add Class documentation for InOutMessageProducer
 *
 * @author sully6768
 */
public class InOutMessageProducer implements SjmsMessageProducer {
    private Session session;
    private MessageProducer messageProducer;

    /**
     * TODO Add Constructor Javadoc
     * 
     * @param session
     * @param messageProducer
     */
    public InOutMessageProducer() {
    }

    /**
     * TODO Add Constructor Javadoc
     * 
     * @param session
     * @param messageProducer
     */
    private InOutMessageProducer(Session session, MessageProducer messageProducer) {
        this.session = session;
        this.messageProducer = messageProducer;
    }

    /** 
     * TODO Add override javadoc
     *
     * @see org.apache.camel.component.sjms.SjmsMessageProducer#createMessageProducer()
     *
     * @return
     * @throws Exception
     */
    @Override
    public InOutMessageProducer createMessageProducer(ConnectionPool connectionPool, String destinationName) throws Exception {
        Connection conn = connectionPool.borrowObject();
        session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        messageProducer = JmsObjectFactory.createQueueProducer(session, destinationName);
        connectionPool.returnObject(conn);
        return new InOutMessageProducer(session, messageProducer);
    }

    /** 
     * TODO Add override javadoc
     *
     * @see org.apache.camel.component.sjms.SjmsMessageProducer#close()
     *
     * @throws Exception
     */
    @Override
    public void destroyMessageProducer() throws Exception {
        if (getMessageProducer() != null) {
            getMessageProducer().close();
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
    
    @Override
    public void send(Message message) throws Exception {
        if (messageProducer != null) {
            messageProducer.send(message);
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
    public MessageProducer getMessageProducer() {
        return messageProducer;
    }

}
