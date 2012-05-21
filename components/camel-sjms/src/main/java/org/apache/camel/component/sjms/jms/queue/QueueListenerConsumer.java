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

import javax.jms.MessageConsumer;

import org.apache.camel.Processor;
import org.apache.camel.component.sjms.jms.DefaultJmsMessageListener;
import org.apache.camel.component.sjms.messagehandlers.DefaultMessageHandler;
import org.apache.camel.component.sjms.pool.QueueConsumerPool;
import org.apache.camel.component.sjms.pool.SessionPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A non-transacted queue consumer for a given JMS Destination
 *
 */
public class QueueListenerConsumer extends QueueConsumer {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());
    private String messageSelector;
    private String destinationName;
    private SessionPool sessions;
    private QueueConsumerPool consumers;
    private int maxConsumers = 1;
    
    public QueueListenerConsumer(QueueEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.sessions = endpoint.getSessions();
        this.maxConsumers = endpoint.getConfiguration().getMaxConsumers();
        setDestinationName(endpoint.getDestinationName());
    }

    protected QueueEndpoint getQueueEndpoint() {
        return (QueueEndpoint)this.getEndpoint();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        consumers = new QueueConsumerPool(maxConsumers, sessions, getDestinationName(), getMessageSelector());
        consumers.fillPool();
        for (int i = 0; i < maxConsumers; i++) {
            MessageConsumer consumer = consumers.borrowObject();
            DefaultJmsMessageListener listener = createMessageListener();
            consumer.setMessageListener(listener);
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (consumers != null)
            consumers.drainPool();
    }

    /**
     * Sets the String value of messageSelector for this instance of SimpleJmsTopicSubscriber.
     *
     * @param messageSelector Sets String, default is TODO add default
     */
    public void setMessageSelector(String messageSelector) {
        this.messageSelector = messageSelector;
    }

    /**
     * Gets the String value of messageSelector for this instance of SimpleJmsTopicSubscriber.
     *
     * @return the messageSelector
     */
    public String getMessageSelector() {
        return messageSelector;
    }

    /**
     * Sets the String value of destinationName for this instance of SimpleJmsConsumer.
     *
     * @param destinationName Sets String, default is TODO add default
     */
    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    /**
     * Gets the String value of destinationName for this instance of SimpleJmsConsumer.
     *
     * @return the destinationName
     */
    public String getDestinationName() {
        return destinationName;
    }

    /**
     * @return
     */
    protected DefaultJmsMessageListener createMessageListener() {
        DefaultMessageHandler messageHandler = null;
        boolean transacted = getQueueEndpoint().getConfiguration().isTransacted();
        if(transacted) {
            
        } else {
            messageHandler = new DefaultMessageHandler();
            messageHandler.setProcessor(getAsyncProcessor());
            messageHandler.setEndpoint(getQueueEndpoint());
            messageHandler.setExceptionHandler(getExceptionHandler());
            messageHandler.setAsync(isAsync());
            messageHandler.setTransacted(isTransacted());
        }
        DefaultJmsMessageListener listener = new DefaultJmsMessageListener(messageHandler);
        return listener;
    }
}
