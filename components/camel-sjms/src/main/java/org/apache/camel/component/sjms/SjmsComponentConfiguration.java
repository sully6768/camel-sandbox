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
package org.apache.camel.component.sjms;

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;

/**
 * TODO Add Class documentation for SjmsComponentConfiguration
 * 
 */
public class SjmsComponentConfiguration {
    
    private SjmsHeaderFilterStrategy sjmsHeaderFilterStrategy;
    private JmsKeyFormatStrategy jmsKeyFormatStrategy;
    private ConnectionFactory connectionFactory;
    private Integer maxConnections = 1;
    private Integer maxSessions = 1;
    private Integer maxConsumers = 1;
    private Integer maxProducers = 1;
    private boolean transacted = false;
    private String acknowledgementMode = "AUTO_ACKNOWLEDGE";
    private boolean durableSubscription = false;
    private int deliveryMode = DeliveryMode.PERSISTENT;
    private boolean disableMessageID = false;
    private boolean disableMessageTimestamp = false;
    private int messagePriority = 10;
    private long messageTimeToLive = -1;

    /**
     * Gets the Integer value of maxConnections for this instance of
     * SjmsComponentConfiguration.
     * 
     * @return the maxConnections
     */
    public final Integer getMaxConnections() {
        return maxConnections;
    }

    /**
     * Sets the Integer value of maxConnections for this instance of
     * SjmsComponentConfiguration.
     * 
     * @param maxConnections
     *            Sets Integer, default is 1
     */
    public final void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * Gets the Integer value of maxSessions for this instance of
     * SjmsComponentConfiguration.
     * 
     * @return the maxSessions
     */
    public final Integer getMaxSessions() {
        return maxSessions;
    }

    /**
     * Sets the Integer value of maxSessions for this instance of
     * SjmsComponentConfiguration.
     * 
     * @param maxSessions
     *            Sets Integer, default is 1
     */
    public final void setMaxSessions(Integer maxSessions) {
        this.maxSessions = maxSessions;
    }

    /**
     * Gets the Integer value of maxConsumers for this instance of
     * SjmsComponentConfiguration.
     * 
     * @return the maxConsumers
     */
    public final Integer getMaxConsumers() {
        return maxConsumers;
    }

    /**
     * Sets the Integer value of maxConsumers for this instance of
     * SjmsComponentConfiguration.
     * 
     * @param maxConsumers
     *            Sets Integer, default is 1
     */
    public final void setMaxConsumers(Integer maxConsumers) {
        this.maxConsumers = maxConsumers;
    }

    /**
     * Sets the ConnectionFactory value of connectionFactory for this instance
     * of SjmsComponentConfiguration.
     * 
     * @param connectionFactory
     *            Sets ConnectionFactory, default is TODO add default
     */
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Gets the ConnectionFactory value of connectionFactory for this instance
     * of SjmsComponentConfiguration.
     * 
     * @return the connectionFactory
     */
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * Sets the Integer value of maxProducers for this instance of
     * SjmsComponentConfiguration.
     * 
     * @param maxProducers
     *            Sets Integer, default is TODO add default
     */
    public void setMaxProducers(Integer maxProducers) {
        this.maxProducers = maxProducers;
    }

    /**
     * Gets the Integer value of maxProducers for this instance of
     * SjmsComponentConfiguration.
     * 
     * @return the maxProducers
     */
    public Integer getMaxProducers() {
        return maxProducers;
    }

    /**
     * Sets the value of transacted for this instance of
     * SjmsComponentConfiguration.
     * 
     * @param transacted
     *            the transacted to set
     */
    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }

    /**
     * Returns the value of transacted for this instance of
     * SjmsComponentConfiguration.
     * 
     * @return the SjmsComponentConfiguration or null
     */
    public boolean isTransacted() {
        return transacted;
    }

    /**
     * Sets the value of durableSubscription for this instance of
     * SjmsComponentConfiguration.
     * 
     * @param durableSubscription
     *            the durableSubscription to set
     */
    public void setDurableSubscription(boolean durableSubscription) {
        this.durableSubscription = durableSubscription;
    }

    /**
     * Returns the value of durableSubscription for this instance of
     * SjmsComponentConfiguration.
     * 
     * @return the SjmsComponentConfiguration or null
     */
    public boolean isDurableSubscription() {
        return durableSubscription;
    }

    /**
     * Sets the value of deliveryMode for this instance of
     * SjmsComponentConfiguration.
     * 
     * @param deliveryMode
     *            the deliveryMode to set
     */
    public void setDeliveryMode(int deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    /**
     * Returns the value of deliveryMode for this instance of
     * SjmsComponentConfiguration.
     * 
     * @return the SjmsComponentConfiguration or null
     */
    public int getDeliveryMode() {
        return deliveryMode;
    }

    /**
     * Sets the value of disableMessageID for this instance of
     * SjmsComponentConfiguration.
     * 
     * @param disableMessageID
     *            the disableMessageID to set
     */
    public void setDisableMessageID(boolean disableMessageID) {
        this.disableMessageID = disableMessageID;
    }

    /**
     * Returns the value of disableMessageID for this instance of
     * SjmsComponentConfiguration.
     * 
     * @return the SjmsComponentConfiguration or null
     */
    public boolean isDisableMessageID() {
        return disableMessageID;
    }

    /**
     * Sets the value of disableMessageTimestamp for this instance of
     * SjmsComponentConfiguration.
     * 
     * @param disableMessageTimestamp
     *            the disableMessageTimestamp to set
     */
    public void setDisableMessageTimestamp(boolean disableMessageTimestamp) {
        this.disableMessageTimestamp = disableMessageTimestamp;
    }

    /**
     * Returns the value of disableMessageTimestamp for this instance of
     * SjmsComponentConfiguration.
     * 
     * @return the SjmsComponentConfiguration or null
     */
    public boolean isDisableMessageTimestamp() {
        return disableMessageTimestamp;
    }

    /**
     * Sets the value of messagePriority for this instance of
     * SjmsComponentConfiguration.
     * 
     * @param messagePriority
     *            the messagePriority to set
     */
    public void setMessagePriority(int messagePriority) {
        this.messagePriority = messagePriority;
    }

    /**
     * Returns the value of messagePriority for this instance of
     * SjmsComponentConfiguration.
     * 
     * @return the SjmsComponentConfiguration or null
     */
    public int getMessagePriority() {
        return messagePriority;
    }

    /**
     * Sets the value of messageTimeToLive for this instance of
     * SjmsComponentConfiguration.
     * 
     * @param messageTimeToLive
     *            the messageTimeToLive to set
     */
    public void setMessageTimeToLive(long messageTimeToLive) {
        this.messageTimeToLive = messageTimeToLive;
    }

    /**
     * Returns the value of messageTimeToLive for this instance of
     * SjmsComponentConfiguration.
     * 
     * @return the SjmsComponentConfiguration or null
     */
    public long getMessageTimeToLive() {
        return messageTimeToLive;
    }

    /**
     * Sets the value of acknowledgementMode for this instance of SjmsComponentConfiguration.
     *
     * @param acknowledgementMode the acknowledgementMode to set
     */
    public void setAcknowledgementMode(String acknowledgementMode) {
        this.acknowledgementMode = acknowledgementMode;
    }

    /**
     * Returns the value of acknowledgementMode for this instance of SjmsComponentConfiguration.
     *
     * @return the SjmsComponentConfiguration or null
     */
    public String getAcknowledgementMode() {
        return acknowledgementMode;
    }

    /**
     * Sets the SjmsHeaderFilterStrategy value of sjmsHeaderFilterStrategy for this instance of SjmsComponentConfiguration.
     *
     * @param sjmsHeaderFilterStrategy Sets SjmsHeaderFilterStrategy, default is TODO add default
     */
    public void setSjmsHeaderFilterStrategy(SjmsHeaderFilterStrategy sjmsHeaderFilterStrategy) {
        this.sjmsHeaderFilterStrategy = sjmsHeaderFilterStrategy;
    }

    /**
     * Gets the SjmsHeaderFilterStrategy value of sjmsHeaderFilterStrategy for this instance of SjmsComponentConfiguration.
     *
     * @return the sjmsHeaderFilterStrategy
     */
    public SjmsHeaderFilterStrategy getSjmsHeaderFilterStrategy() {
        return sjmsHeaderFilterStrategy;
    }

    /**
     * Sets the JmsKeyFormatStrategy value of jmsKeyFormatStrategy for this instance of SjmsComponentConfiguration.
     *
     * @param jmsKeyFormatStrategy Sets JmsKeyFormatStrategy, default is TODO add default
     */
    public void setJmsKeyFormatStrategy(JmsKeyFormatStrategy jmsKeyFormatStrategy) {
        this.jmsKeyFormatStrategy = jmsKeyFormatStrategy;
    }

    /**
     * Gets the JmsKeyFormatStrategy value of jmsKeyFormatStrategy for this instance of SjmsComponentConfiguration.
     *
     * @return the jmsKeyFormatStrategy
     */
    public JmsKeyFormatStrategy getJmsKeyFormatStrategy() {
        return jmsKeyFormatStrategy;
    }

}
