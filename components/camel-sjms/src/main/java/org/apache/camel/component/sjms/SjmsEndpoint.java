/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.sjms;

import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.ExchangePattern;
import org.apache.camel.MultipleConsumersSupport;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.sjms.consumer.DefaultConsumer;
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
    private SessionAcknowledgementType acknowledgementMode = SessionAcknowledgementType.AUTO_ACKNOWLEDGE;
    private boolean topic = false;
    private int sessionCount = 1;
    private int producerCount = 1;
    private int consumerCount = 1;
    private long ttl = -1;
    private boolean persistent = true;
    private String durableSubscription;
    

    public SjmsEndpoint() {
        super();
    }

    public SjmsEndpoint(String uri, Component component) {
        super(uri, component);
        if (getEndpointUri().indexOf("://queue:") > -1) {
            setTopic(false);
        } else if (getEndpointUri().indexOf("://topic:") > -1) {
            setTopic(true);
        }  else {
            throw new RuntimeCamelException("Endpoint URI unsupported: " + uri);
        }
        if (isTransacted()) {
            setAcknowledgementMode(SessionAcknowledgementType.SESSION_TRANSACTED);
        }
        setConfiguration(((SjmsComponent)component).getConfiguration());
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
        
        //
        // TODO since we only need a session pool for one use case, find a better way
        //
        // We only create a session pool when we are not transacted.
        // Transacted listeners or producers need to be paired with the
        // Session that created them.
        if( ! isTransacted()) {
            sessions = new SessionPool(getSessionCount(), getConnections());
            
            // TODO fix the string hack
            sessions.setAcknowledgeMode(SessionAcknowledgementType
                    .valueOf(getAcknowledgementMode()+""));
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
        } else {
            producer = new InOutProducer(this);
        }
        return producer;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new DefaultConsumer(this, processor);
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
    
    public void setConfiguration(SjmsComponentConfiguration configuration) {
        this.configuration = configuration;
    }

    public SjmsComponentConfiguration getConfiguration() {
        return configuration;
    }

    public void setConnections(ConnectionPool connections) {
        this.connections = connections;
    }

    public ConnectionPool getConnections() {
        return connections;
    }

    public SessionPool getSessions() {
        return sessions;
    }

    public SjmsHeaderFilterStrategy getSjmsHeaderFilterStrategy() {
        return getConfiguration().getSjmsHeaderFilterStrategy();
    }

    public JmsKeyFormatStrategy getJmsKeyFormatStrategy() {
        return getConfiguration().getJmsKeyFormatStrategy();
    }

    public void setMessageHandler(SjmsMessageConsumer sjmsMessageConsumer) {
        this.sjmsMessageConsumer = sjmsMessageConsumer;
    }

    public SjmsMessageConsumer getMessageHandler() {
        return sjmsMessageConsumer;
    }

    public void setSynchronous(boolean asyncConsumer) {
        this.synchronous = asyncConsumer;
    }

    public boolean isSynchronous() {
        return synchronous;
    }

    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }

    public boolean isTransacted() {
        return transacted;
    }

    public void setNamedReplyTo(String namedReplyTo) {
        this.namedReplyTo = namedReplyTo;
        this.setExchangePattern(ExchangePattern.InOut);
    }

    public String getNamedReplyTo() {
        return namedReplyTo;
    }

    public void setAcknowledgementMode(SessionAcknowledgementType acknowledgementMode) {
        this.acknowledgementMode = acknowledgementMode;
    }

    public SessionAcknowledgementType getAcknowledgementMode() {
        return acknowledgementMode;
    }

    public void setTopic(boolean topic) {
        this.topic = topic;
    }

    public boolean isTopic() {
        return topic;
    }

    public void setProducerCount(int producerCount) {
        this.producerCount = producerCount;
    }

    public int getProducerCount() {
        return producerCount;
    }

    public void setConsumerCount(int consumerCount) {
        this.consumerCount = consumerCount;
    }

    public int getConsumerCount() {
        return consumerCount;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public String getDurableSubscription() {
        return durableSubscription;
    }

    public void setDurableSubscription(String durableSubscription) {
        this.durableSubscription = durableSubscription;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }
}
