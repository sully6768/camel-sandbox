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
package org.apache.camel.component.sjms;

import javax.jms.Session;

import org.apache.camel.Consumer;
import org.apache.camel.ExchangePattern;
import org.apache.camel.MultipleConsumersSupport;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.sjms.consumer.QueueConsumer;
import org.apache.camel.component.sjms.consumer.QueueListenerConsumer;
import org.apache.camel.component.sjms.consumer.TransactedQueueListenerConsumer;
import org.apache.camel.component.sjms.jms.SessionAcknowledgementType;
import org.apache.camel.component.sjms.pool.ConnectionPool;
import org.apache.camel.component.sjms.pool.SessionPool;
import org.apache.camel.component.sjms.producer.InOnlyProducer;
import org.apache.camel.component.sjms.producer.InOutProducer;
import org.apache.camel.impl.DefaultEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Add Class documentation for SjmsEndpoint
 *
 */
public class SjmsEndpoint extends DefaultEndpoint implements MultipleConsumersSupport {
    protected final transient Logger logger = LoggerFactory
            .getLogger(getClass());

    private SjmsComponentConfiguration configuration;
    private ConnectionPool connections;
    private SessionPool sessions;
    private SjmsMessageConsumer sjmsMessageConsumer;
    private boolean synchronous = true;
    private boolean transacted = false;
    private String namedReplyTo;
    private int acknowledgementMode = Session.AUTO_ACKNOWLEDGE;
    private boolean topic = false;
    private int producerCount = 1;
    private int consumerCount = 1;
    private long ttl = -1;
    private boolean persistent = true;
    private String durableSubscription;

    public SjmsEndpoint() {
        setExchangePattern(ExchangePattern.InOnly);
    }

    public SjmsEndpoint(String uri, SjmsComponent component) {
        super(uri, component);
        if (getEndpointUri().indexOf("://queue:") > -1) {
            setTopic(false);
        } else if (getEndpointUri().indexOf("://topic:") > -1) {
            setTopic(true);
        }  else {
            throw new RuntimeCamelException("Endpoint URI unsupported: " + uri);
        }
        if (isTransacted()) {
            setAcknowledgementMode(Session.SESSION_TRANSACTED);
        }
        setConfiguration(component.getConfiguration());
        setExchangePattern(ExchangePattern.InOnly);
    }
    
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        
        // Start with some parameter validation and overriding
        // First check for 
        
        // We always use a connection pool, even for a pool of 1
        connections = new ConnectionPool(getConfiguration()
                .getMaxConnections(), getConfiguration().getConnectionFactory());
        connections.fillPool();
        
        // We only create a session pool when we are not transacted.
        // Transacted listeners or producers need to be paired with the
        // Session that created them.
        if( ! isTransacted()) {
            sessions = new SessionPool(getConfiguration()
                    .getMaxSessions(), getConnections());
            sessions.setAcknowledgeMode(SessionAcknowledgementType
                    .valueOf(getConfiguration().getAcknowledgementMode()));
            getSessions().fillPool();   
        }
    }
    
    @Override
    protected void doStop() throws Exception {
        if (getSessions() != null) {
            getSessions().drainPool();
        }
        getConnections().drainPool();
        super.doStop();
    }

    @Override
    public Producer createProducer() throws Exception {
        SjmsProducer producer = null;
        if (this.getExchangePattern().equals(ExchangePattern.InOnly)) {
            producer = new InOnlyProducer(this);
        } else if (this.getExchangePattern().equals(ExchangePattern.InOut)) {
            producer = new InOutProducer(this);
        }
        return producer;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        QueueConsumer answer = null;
        if(isTransacted()) {
            answer = new TransactedQueueListenerConsumer(this, processor);
        } else {
            answer = new QueueListenerConsumer(this, processor);
        }
        return answer;
    }
    
    @Override
    public boolean isMultipleConsumersSupported() {
        return true;
    }
    
    @Override
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
     * Returns the value of sessions for this instance of
     * SimpleJmsQueueEndpoint.
     * 
     * @return the SimpleJmsQueueEndpoint or null
     */
    public SessionPool getSessions() {
        return sessions;
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
     * Sets the SjmsMessageConsumer value of sjmsMessageConsumer for this instance of
     * SimpleJmsEndpoint.
     * 
     * @param sjmsMessageConsumer
     *            Sets SjmsMessageConsumer, default is TODO add default
     */
    public void setMessageHandler(SjmsMessageConsumer sjmsMessageConsumer) {
        this.sjmsMessageConsumer = sjmsMessageConsumer;
    }

    /**
     * Gets the SjmsMessageConsumer value of sjmsMessageConsumer for this instance of
     * SimpleJmsEndpoint.
     * 
     * @return the sjmsMessageConsumer
     */
    public SjmsMessageConsumer getMessageHandler() {
        return sjmsMessageConsumer;
    }

    /**
     * Sets the boolean value of synchronous for this instance of SjmsEndpoint.
     *
     * @param synchronous Sets boolean, default is TODO add default
     */
    public void setSynchronous(boolean asyncConsumer) {
        this.synchronous = asyncConsumer;
    }

    /**
     * Gets the boolean value of synchronous for this instance of SjmsEndpoint.
     *
     * @return the synchronous
     */
    public boolean isSynchronous() {
        return synchronous;
    }

    /**
     * Sets the boolean value of transacted for this instance of SjmsEndpoint.
     *
     * @param transacted Sets boolean, default is TODO add default
     */
    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }

    /**
     * Gets the boolean value of transacted for this instance of SjmsEndpoint.
     *
     * @return the transacted
     */
    public boolean isTransacted() {
        return transacted;
    }

    /**
     * Sets the String value of namedReplyTo for this instance of SjmsEndpoint.
     *
     * @param namedReplyTo Sets the value of the namedReplyTo attribute
     */
    public void setNamedReplyTo(String namedReplyTo) {
        this.namedReplyTo = namedReplyTo;
        this.setExchangePattern(ExchangePattern.InOut);
    }

    /**
     * Gets the String value of namedReplyTo for this instance of SjmsEndpoint.
     *
     * @return the namedReplyTo
     */
    public String getNamedReplyTo() {
        return namedReplyTo;
    }

    /**
     * Sets the String value of messageExchangePattern for this instance of SjmsEndpoint.
     *
     * @param messageExchangePattern Sets String, default is TODO add default
     */
    public void setMessageExchangePattern(ExchangePattern messageExchangePattern) {
        if(messageExchangePattern.equals(ExchangePattern.InOut)) {
            this.setExchangePattern(ExchangePattern.InOut);
        } else if(messageExchangePattern.equals(ExchangePattern.RobustInOnly)) {
            this.setExchangePattern(ExchangePattern.RobustInOnly);
        } else if(messageExchangePattern.equals(ExchangePattern.InOnly)) {
            this.setExchangePattern(ExchangePattern.InOnly);
        } else {
            throw new RuntimeCamelException("The MEP " + messageExchangePattern + " is not supported by the Simple JMS Endpoint");
        }
        this.setExchangePattern(messageExchangePattern);
    }

    /**
     * Gets the String value of messageExchangePattern for this instance of SjmsEndpoint.
     *
     * @return the messageExchangePattern
     */
    public ExchangePattern getMessageExchangePattern() {
        return getExchangePattern();
    }

    /**
     * Sets the int value of acknowledgementMode for this instance of SjmsEndpoint.
     *
     * @param acknowledgementMode Sets int, default is TODO add default
     */
    public void setAcknowledgementMode(int acknowledgementMode) {
        this.acknowledgementMode = acknowledgementMode;
    }

    /**
     * Gets the int value of acknowledgementMode for this instance of SjmsEndpoint.
     *
     * @return the acknowledgementMode
     */
    public int getAcknowledgementMode() {
        return acknowledgementMode;
    }

    /**
     * Sets the boolean value of topic for this instance of SjmsEndpoint.
     *
     * @param topic Sets boolean, default is TODO add default
     */
    public void setTopic(boolean topic) {
        this.topic = topic;
    }

    /**
     * Gets the boolean value of topic for this instance of SjmsEndpoint.
     *
     * @return the topic
     */
    public boolean isTopic() {
        return topic;
    }

    /**
     * Sets the int value of producerCount for this instance of SjmsEndpoint.
     *
     * @param producerCount Sets int, default is TODO add default
     */
    public void setProducerCount(int producerCount) {
        this.producerCount = producerCount;
    }

    /**
     * Gets the int value of producerCount for this instance of SjmsEndpoint.
     *
     * @return the producerCount
     */
    public int getProducerCount() {
        return producerCount;
    }

    /**
     * Sets the int value of consumerCount for this instance of SjmsEndpoint.
     *
     * @param consumerCount Sets int, default is TODO add default
     */
    public void setConsumerCount(int consumerCount) {
        this.consumerCount = consumerCount;
    }

    /**
     * Gets the int value of consumerCount for this instance of SjmsEndpoint.
     *
     * @return the consumerCount
     */
    public int getConsumerCount() {
        return consumerCount;
    }

    /**
     * Gets the long value of ttl for this instance of SjmsEndpoint.
     *
     * @return the ttl
     */
    public long getTtl() {
        return ttl;
    }

    /**
     * Sets the long value of ttl for this instance of SjmsEndpoint.
     *
     * @param ttl Sets long, default is TODO add default
     */
    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    /**
     * Gets the boolean value of persistent for this instance of SjmsEndpoint.
     *
     * @return the persistent
     */
    public boolean isPersistent() {
        return persistent;
    }

    /**
     * Sets the boolean value of persistent for this instance of SjmsEndpoint.
     *
     * @param persistent Sets boolean, default is TODO add default
     */
    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    /**
     * Gets the String value of durableSubscription for this instance of SjmsEndpoint.
     *
     * @return the durableSubscription
     */
    public String getDurableSubscription() {
        return durableSubscription;
    }

    /**
     * Sets the String value of durableSubscription for this instance of SjmsEndpoint.
     *
     * @param durableSubscription Sets String, default is TODO add default
     */
    public void setDurableSubscription(String durableSubscription) {
        this.durableSubscription = durableSubscription;
    }
}
