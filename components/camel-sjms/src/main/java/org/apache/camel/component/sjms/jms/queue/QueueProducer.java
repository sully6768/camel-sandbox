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

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.QueueSender;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.component.sjms.JmsMessageHelper;
import org.apache.camel.component.sjms.JmsMessageType;
import org.apache.camel.component.sjms.pool.QueueProducerPool;
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

    private String destinationName = null;
    private QueueProducerPool producers;
    private int maxProducers = 1;
    private boolean transacted = false;
    private boolean async = false;

    public QueueProducer(QueueEndpoint endpoint) {
        super(endpoint);
    }

    protected QueueEndpoint getQueueEndpoint() {
        return (QueueEndpoint)this.getEndpoint();
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        if (logger.isDebugEnabled()) {
            logger.info((String) exchange.getIn().getBody());
        }
        
        try {
            Message message = createMessage(exchange);
            QueueSender sender = producers.borrowObject();
            try {
                sender.send(message);
            } catch (JMSException e) {
                exchange.setException(e);
            }
            producers.returnObject(sender);
        } catch (Exception e) {
            exchange.setException(e);
        }
        callback.done(true);
        return true;
    }
    
    @SuppressWarnings("unchecked")
    private Message createMessage(Exchange exchange) throws Exception {
        Message answer = null;
        Object body = exchange.getIn().getBody();
        JmsMessageType messageType = JmsMessageHelper.discoverType(exchange);
        switch (messageType) {
        case Bytes:
            BytesMessage bytesMessage = createBytesMessage();
            bytesMessage.writeBytes((byte[]) body);
            answer = bytesMessage;
            break;
        case Map:
            MapMessage mapMessage = createMapMessage();
            Map<String, Object> objMap = (Map<String, Object>) body;
            Set<String> keys = objMap.keySet();
            for (String key : keys) {
                Object value = objMap.get(key);
                mapMessage.setObject(key, value);
            }
            mapMessage.setObject(destinationName, mapMessage);
            break;
        case Object:
            ObjectMessage objectMessage = createObjectMessage();
            objectMessage.setObject((Serializable) body);
            answer = objectMessage;
            break;
        case Text:
            TextMessage textMessage = createTextMessage();
            textMessage.setText((String) body);
            answer = textMessage;
            break;
        default:
            break;
        }
        
        answer = JmsMessageHelper.setJmsMessageHeaders(exchange, answer);
        return answer;
    }
    
    private TextMessage createTextMessage() throws Exception {
        Session s = getSessionPool().borrowObject();
        TextMessage textMessage = s.createTextMessage();
        getSessionPool().returnObject(s);
        return textMessage;
    }
    
    private MapMessage createMapMessage() throws Exception {
        Session s = getSessionPool().borrowObject();
        MapMessage mapMessage = s.createMapMessage();
        getSessionPool().returnObject(s);
        return mapMessage;
    }
    
    private ObjectMessage createObjectMessage() throws Exception {
        Session s = getSessionPool().borrowObject();
        ObjectMessage objectMessage = s.createObjectMessage();
        getSessionPool().returnObject(s);
        return objectMessage;
    }
    
    private BytesMessage createBytesMessage() throws Exception {
        Session s = getSessionPool().borrowObject();
        BytesMessage bytesMessage = s.createBytesMessage();
        getSessionPool().returnObject(s);
        return bytesMessage;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        producers = new QueueProducerPool(maxProducers, this.getQueueEndpoint().getSessions(), this.getQueueEndpoint().getDestinationName());
        producers.fillPool();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        producers.drainPool();
        producers = null;
    }
    
    protected SessionPool getSessionPool() {
        return getQueueEndpoint().getSessions();
    }

    /**
     * Sets the boolean value of transacted for this instance of QueueProducer.
     *
     * @param transacted Sets boolean, default is TODO add default
     */
    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }

    /**
     * Gets the boolean value of transacted for this instance of QueueProducer.
     *
     * @return the transacted
     */
    public boolean isTransacted() {
        return transacted;
    }

}
