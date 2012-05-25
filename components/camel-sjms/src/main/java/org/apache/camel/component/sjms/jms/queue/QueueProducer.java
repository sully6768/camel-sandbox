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

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.jms.BytesMessage;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.component.sjms.JmsMessageHelper;
import org.apache.camel.component.sjms.JmsMessageType;
import org.apache.camel.component.sjms.pool.ConnectionPool;
import org.apache.camel.component.sjms.pool.ObjectPool;
import org.apache.camel.component.sjms.pool.SessionPool;
import org.apache.camel.impl.DefaultAsyncProducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Add Class documentation for QueueConsumer
 *
 */
public class QueueProducer extends DefaultAsyncProducer {
    protected final transient Logger logger = LoggerFactory.getLogger(getClass());
    private final ExecutorService executor;

    private QueueProducerPool producers;
    
    private class QueueProducerPool extends ObjectPool<QueueSender>{

        /**
         * TODO Add Constructor Javadoc
         *
         * @param sessionPool
         */
        public QueueProducerPool() {
            super(getMaxProducers());
        }

        @Override
        protected QueueSender createObject() throws Exception {
            QueueSession queueSession = (QueueSession) getSessionPool().borrowObject();
            Queue myQueue = queueSession.createQueue(getDestinationName());
            QueueSender queueSender = queueSession.createSender(myQueue);
            getSessionPool().returnObject(queueSession);
            return queueSender;
        }
        
        @Override
        protected void destroyObject(QueueSender queueSender) throws Exception {
            if (queueSender != null) {
                queueSender.close();
            }
        }
    }
    
    public QueueProducer(QueueEndpoint endpoint) {
        super(endpoint);
        this.executor = endpoint.getCamelContext().getExecutorServiceManager().newDefaultThreadPool(this, "MyProducer");
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if(getSessionPool() != null) {
            producers = new QueueProducerPool();
            producers.fillPool();
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (producers != null) {
            producers.drainPool();
            producers = null;   
        }
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        if (log.isDebugEnabled()) {
            log.info((String) exchange.getIn().getBody());
        }
        if(exchange.getPattern() == ExchangePattern.RobustInOnly) {
            // process process the InOnly request 
            // and in the case of an exception propagate it to the exchange
            return processRobustInOnly(exchange, callback);
        } else if(exchange.getPattern() == ExchangePattern.InOut) {
            // process request reply
            return processInOut(exchange, callback);
        } else {
            // Default is to process the InOnly pattern where
            // errors are not propagated to the Exchange
            return processInOnly(exchange, callback);
        }
    }
    
    protected boolean processInOnly(Exchange exchange, AsyncCallback callback) {
        if(isAsync()) {
            if(log.isDebugEnabled()) {
                log.debug("Begin Async-InOnly Producer processing of Exchange id:{}", exchange.getExchangeId());
            }
            try {
                sendAsyncMessage(exchange, callback);
            } catch (Exception e) {
                exchange.setException(e);
            }
            callback.done(false);
            if(log.isDebugEnabled()) {
                log.debug("Finish Async-InOnly Producer processing of Exchange id:{}", exchange.getExchangeId());
            }
            return false;
        } else {
            if(log.isDebugEnabled()) {
                log.debug("Begin Sync-InOnly Producer processing of Exchange id:{}", exchange.getExchangeId());
            }
            try {
                sendMessage(exchange);
            } catch (Exception e) {
                exchange.setException(e);
            }
            callback.done(true);
            if(log.isDebugEnabled()) {
                log.debug("Finish Sync-InOnly Producer processing of Exchange id:{}", exchange.getExchangeId());
            }
            return true;
        }
    }
    
    protected boolean processRobustInOnly(Exchange exchange, AsyncCallback callback) {
        try {
            sendMessage(exchange);
        } catch (Exception e) {
            exchange.setException(e);
        }
        callback.done(isAsync());
        return isAsync();
    }
    
    protected boolean processInOut(Exchange exchange, AsyncCallback callback) {
        try {
            sendMessage(exchange);
        } catch (Exception e) {
            exchange.setException(e);
        }
        callback.done(isAsync());
        return isAsync();
    }
    
    protected void sendMessage(Exchange exchange) throws Exception {
        Session session = getSessionPool().borrowObject();
        Message message = createMessage(exchange, session);
        getSessionPool().returnObject(session);
        if (producers != null) {
            QueueSender sender = producers.borrowObject();
            sender.send(message);
            producers.returnObject(sender);
        }
    }
    
    protected void sendAsyncMessage(final Exchange exchange, final AsyncCallback callback) {
        executor.submit(new Callable<Object>() {
            public Object call() throws Exception {
                sendMessage(exchange);
                log.info("Callback done(false)");
                callback.done(false);
                return null;
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    protected Message createMessage(Exchange exchange, Session session) throws Exception {
        Message answer = null;
        Object body = exchange.getIn().getBody();
        JmsMessageType messageType = JmsMessageHelper.discoverType(exchange);

        switch (messageType) {
        case Bytes:
            BytesMessage bytesMessage = session.createBytesMessage();
            bytesMessage.writeBytes((byte[]) body);
            answer = bytesMessage;
            break;
        case Map:
            MapMessage mapMessage = session.createMapMessage();
            Map<String, Object> objMap = (Map<String, Object>) body;
            Set<String> keys = objMap.keySet();
            for (String key : keys) {
                Object value = objMap.get(key);
                mapMessage.setObject(key, value);
            }
            mapMessage.setObject(getDestinationName(), mapMessage);
            break;
        case Object:
            ObjectMessage objectMessage = session.createObjectMessage();
            objectMessage.setObject((Serializable) body);
            answer = objectMessage;
            break;
        case Text:
            TextMessage textMessage = session.createTextMessage();
            textMessage.setText((String) body);
            answer = textMessage;
            break;
        default:
            break;
        }
        
        answer = JmsMessageHelper.setJmsMessageHeaders(exchange, answer);
        return answer;
    }

    protected QueueEndpoint getQueueEndpoint() {
        return (QueueEndpoint)this.getEndpoint();
    }
    
    protected ConnectionPool getConnectionPool() {
        return getQueueEndpoint().getConnections();
    }
    
    protected SessionPool getSessionPool() {
        return getQueueEndpoint().getSessions();
    }

    /**
     * Gets the boolean value of async for this instance of QueueProducer.
     *
     * @return the async
     */
    public boolean isAsync() {
        return getQueueEndpoint().isAsyncProducer();
    }

    /**
     * Gets the String value of replyTo for this instance of QueueProducer.
     *
     * @return the replyTo
     */
    public String getReplyTo() {
        return getQueueEndpoint().getNamedReplyTo();
    }

    /**
     * Gets the String value of destinationName for this instance of QueueProducer.
     *
     * @return the destinationName
     */
    public String getDestinationName() {
        return getQueueEndpoint().getDestinationName();
    }

    /**
     * Gets the int value of maxProducers for this instance of QueueProducer.
     *
     * @return the maxProducers
     */
    public int getMaxProducers() {
        return getQueueEndpoint().getMaxProducers();
    }

}
