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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.component.sjms.MessageHandler;
import org.apache.camel.component.sjms.messagehandlers.InOnlyMessageHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A non-transacted queue consumer for a given JMS Destination
 *
 */
public class QueueListenerConsumer extends QueueConsumer {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());
    protected final transient ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    
    private ConcurrentHashMap<String, MessageHandler> messageHandlers = new ConcurrentHashMap<String, MessageHandler>();
    private ConcurrentHashMap<String, MessageConsumer> messageConsumers = new ConcurrentHashMap<String, MessageConsumer>();
    private AtomicBoolean stopped = new AtomicBoolean(false);
    
    public QueueListenerConsumer(QueueEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if(getSessionPool() != null) {
            for (int i = 0; i < getMaxConsumers(); i++) {
                
                QueueSession queueSession = (QueueSession) getSessionPool().borrowObject();
                QueueReceiver messageConsumer = createConsumer(queueSession);
                getSessionPool().returnObject(queueSession);
                MessageHandler handler = createMessageHandler();
                
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
    }

    @Override
    protected void doStop() throws Exception {
        getStopped().set(true);
        lock.writeLock().lock();
        try {
            Set<String> keys = getMessageHandlers().keySet();
            for (String key : keys) {
                if (getMessageHandlers().contains(key)) {
                    MessageHandler messageHandler = getMessageHandlers().remove(key);
                    messageHandler.close();
                }
                if (getMessageConsumers().containsKey(key)) {
                    MessageConsumer messageConsumer = getMessageConsumers().remove(key);
                    messageConsumer.close();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        super.doStop();
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

    /**
     * @param queueSession
     * @return
     * @throws JMSException
     */
    protected QueueReceiver createConsumer(QueueSession queueSession)
            throws JMSException {
        QueueReceiver messageConsumer;
        Queue myQueue = queueSession.createQueue(getDestinationName());
        messageConsumer = queueSession.createReceiver(myQueue);
        return messageConsumer;
    }
    
    /**
     * @return
     */
    protected MessageHandler createMessageHandler() {
        return createMessageHandler(null);
    }
    
    /**
     * @param session
     * @return
     */
    protected MessageHandler createMessageHandler(Session session) {
        MessageHandler answer = null;
        if (getQueueEndpoint().getExchangePattern().equals(ExchangePattern.InOnly) ){
            InOnlyMessageHandler messageHandler = new InOnlyMessageHandler(getStopped());
            messageHandler.setSession(session);
            messageHandler.setProcessor(getAsyncProcessor());
            messageHandler.setEndpoint(getQueueEndpoint());
            messageHandler.setAsync(isAsync());
            messageHandler.setTransacted(isTransacted());
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

    /**
     * Sets the ConcurrentHashMap<String,MessageHandler> value of
     * messageHandlers for this instance of QueueListenerConsumer.
     * 
     * @param messageHandlers
     *            Sets ConcurrentHashMap<String,MessageHandler>, default is TODO
     *            add default
     */
    public void setMessageHandlers(
            ConcurrentHashMap<String, MessageHandler> messageHandlers) {
        this.messageHandlers = messageHandlers;
    }

    /**
     * Gets the ConcurrentHashMap<String,MessageHandler> value of
     * messageHandlers for this instance of QueueListenerConsumer.
     * 
     * @return the messageHandlers
     */
    public ConcurrentHashMap<String, MessageHandler> getMessageHandlers() {
        return messageHandlers;
    }

    /**
     * Sets the ConcurrentHashMap<String,MessageConsumer> value of
     * messageConsumers for this instance of QueueListenerConsumer.
     * 
     * @param messageConsumers
     *            Sets ConcurrentHashMap<String,MessageConsumer>, default is
     *            TODO add default
     */
    public void setMessageConsumers(
            ConcurrentHashMap<String, MessageConsumer> messageConsumers) {
        this.messageConsumers = messageConsumers;
    }

    /**
     * Gets the ConcurrentHashMap<String,MessageConsumer> value of
     * messageConsumers for this instance of QueueListenerConsumer.
     * 
     * @return the messageConsumers
     */
    public ConcurrentHashMap<String, MessageConsumer> getMessageConsumers() {
        return messageConsumers;
    }
}
