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

import org.apache.camel.Consumer;
import org.apache.camel.MultipleConsumersSupport;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.sjms.JmsKeyFormatStrategy;
import org.apache.camel.component.sjms.MessageHandler;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.component.sjms.SjmsComponentConfiguration;
import org.apache.camel.component.sjms.SjmsHeaderFilterStrategy;
import org.apache.camel.component.sjms.jms.SessionAcknowledgementType;
import org.apache.camel.component.sjms.pool.ConnectionPool;
import org.apache.camel.component.sjms.pool.SessionPool;
import org.apache.camel.impl.DefaultEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Add Class documentation for QueueListenerConsumer
 *
 */
public class QueueEndpoint extends DefaultEndpoint implements
MultipleConsumersSupport{
    protected final transient Logger logger = LoggerFactory
            .getLogger(getClass());

    private SjmsComponentConfiguration configuration;

    private ConnectionPool connections;

    private SessionPool sessions;

    private MessageHandler messageHandler;
    
    private boolean asyncConsumer = false;
    private boolean transacted = false;

    /*
     * Cache All Consumers
     * 
     * If the endpoint is a consumer
     *   If the endpoint is non-transacted
     *     create a QueueListenerConsumer
     *   If the endpoint is local transaction
     *     create a QueueListenerConsumer w/ Dedicated Session
     *   If the endpoint is an XA Transaction
     *     create a QueuePollingConsumer w/ Dedicated Session & Transaction Manager
     *     Pass the XID along the route
     *     Consumers close the route and publis the close event to the seda queue listener to close the producer
     *   If the endpoint is Request/Reply
     *     TBD
     */
    
    /*
     * Cache all producers
     * 
     * If the endpoint is a producer
     *   If the endpoint is non-transacted
     *     create a QueueProducer
     *   If the endpoint is local transaction
     *     create a QueueProducer w/ Dedicated Session
     *   If the endpoint is an XA Transaction
     *     create a QueueProducer w/ Dedicated Session and the Transaction Manager
     *     Create a XID
     *     If reference endpoint exists
     *     if the endpoint is a new XA
     *       Start new XA Process
     *       Create a XID
     *       Create a XID Seda Queue Listener
     *       
     *       
     */


    public QueueEndpoint() {
    }

    public QueueEndpoint(String uri, SjmsComponent component) {
        super(uri, component);
        this.setConfiguration(component.getConfiguration());
        setConnections(new ConnectionPool(getConfiguration()
                .getMaxConnections(), getConfiguration().getConnectionFactory()));
        SessionPool sessions = new SessionPool(getConfiguration()
                .getMaxSessions(), getConnections());
        sessions.setAcknowledgeMode(SessionAcknowledgementType
                .valueOf(getConfiguration().getAcknowledgementMode()));
        sessions.setTransacted(getConfiguration().isTransacted());
        setSessions(sessions);
    }

    @Override
    public Producer createProducer() throws Exception {
        QueueProducer answer = new QueueProducer(this);
        return answer;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        QueueConsumer answer = null;
        if(getConfiguration().isTransacted()) {
            
        } else {
            answer = new QueueListenerConsumer(this, processor);  
        }
        if(getConfiguration().getMaxConsumers() > 1) {
            setAsyncConsumer(true);
        }
        answer.setAsync(isAsyncConsumer());
        answer.setTransacted(isTransacted());
        return answer;
    }

    public boolean isSingleton() {
        return true;
    }

    /**
     * @param endpoint
     */
    public String getDestinationName() {
        return getEndpointUri()
                .substring(getEndpointUri().lastIndexOf(":") + 1);
    }

    /**
     * Sets the SjmsComponentConfiguration value of configuration for this
     * instance of SimpleJmsEndpoint.
     * 
     * @param configuration
     *            Sets SjmsComponentConfiguration, default is TODO add
     *            default
     */
    public void setConfiguration(SjmsComponentConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Gets the SjmsComponentConfiguration value of configuration for this
     * instance of SimpleJmsEndpoint.
     * 
     * @return the configuration
     */
    public SjmsComponentConfiguration getConfiguration() {
        return configuration;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.camel.MultipleConsumersSupport#isMultipleConsumersSupported()
     */
    @Override
    public boolean isMultipleConsumersSupported() {
        return true;
    }

    /**
     * 
     * @see org.apache.camel.impl.DefaultEndpoint#doStart()
     */
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        getConnections().fillPool();
        getSessions().fillPool();
    }

    /**
     * 
     * @see org.apache.camel.impl.DefaultEndpoint#doStop()
     */
    @Override
    protected void doStop() throws Exception {
        super.doStop();
        getSessions().drainPool();
        getConnections().drainPool();
    }

    /**
     * Sets the value of connections for this instance of
     * SimpleJmsQueueEndpoint.
     * 
     * @param connections
     *            the connections to set
     */
    public void setConnections(ConnectionPool connections) {
        this.connections = connections;
    }

    /**
     * Returns the value of connections for this instance of
     * SimpleJmsQueueEndpoint.
     * 
     * @return the SimpleJmsQueueEndpoint or null
     */
    public ConnectionPool getConnections() {
        return connections;
    }

    /**
     * Sets the value of sessions for this instance of SimpleJmsQueueEndpoint.
     * 
     * @param sessions
     *            the sessions to set
     */
    public void setSessions(SessionPool sessions) {
        this.sessions = sessions;
    }

    /**
     * Returns the value of sessions for this instance of
     * SimpleJmsQueueEndpoint.
     * 
     * @return the SimpleJmsQueueEndpoint or null
     */
    public SessionPool getSessions() {
        return sessions;
    }

    /**
     * @return
     */
    protected int getMaxProducers() {
        int maxProducers = ((SjmsComponent) this.getComponent())
                .getConfiguration().getMaxProducers();
        return maxProducers;
    }

    /**
     * Gets the SjmsHeaderFilterStrategy value of sjmsHeaderFilterStrategy for
     * this instance of SjmsComponentConfiguration.
     * 
     * @return the sjmsHeaderFilterStrategy
     */
    public SjmsHeaderFilterStrategy getSjmsHeaderFilterStrategy() {
        return getConfiguration().getSjmsHeaderFilterStrategy();
    }

    /**
     * Gets the JmsKeyFormatStrategy value of jmsKeyFormatStrategy for this
     * instance of SjmsComponentConfiguration.
     * 
     * @return the jmsKeyFormatStrategy
     */
    public JmsKeyFormatStrategy getJmsKeyFormatStrategy() {
        return getConfiguration().getJmsKeyFormatStrategy();
    }

    /**
     * Sets the MessageHandler value of messageHandler for this instance of
     * SimpleJmsEndpoint.
     * 
     * @param messageHandler
     *            Sets MessageHandler, default is TODO add default
     */
    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Gets the MessageHandler value of messageHandler for this instance of
     * SimpleJmsEndpoint.
     * 
     * @return the messageHandler
     */
    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    /**
     * Sets the boolean value of asyncConsumer for this instance of QueueEndpoint.
     *
     * @param asyncConsumer Sets boolean, default is TODO add default
     */
    public void setAsyncConsumer(boolean asyncConsumer) {
        this.asyncConsumer = asyncConsumer;
    }

    /**
     * Gets the boolean value of asyncConsumer for this instance of QueueEndpoint.
     *
     * @return the asyncConsumer
     */
    public boolean isAsyncConsumer() {
        return asyncConsumer;
    }

    /**
     * Sets the boolean value of transacted for this instance of QueueEndpoint.
     *
     * @param transacted Sets boolean, default is TODO add default
     */
    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }

    /**
     * Gets the boolean value of transacted for this instance of QueueEndpoint.
     *
     * @return the transacted
     */
    public boolean isTransacted() {
        return transacted;
    }
}
