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

import javax.jms.ConnectionFactory;

/**
 * TODO Add Class documentation for SjmsComponentConfiguration
 * 
 */
public class SjmsComponentConfiguration {

    private SjmsHeaderFilterStrategy sjmsHeaderFilterStrategy;
    private JmsKeyFormatStrategy jmsKeyFormatStrategy;
    private ConnectionFactory connectionFactory;
    private Integer maxConnections = 1;

    public SjmsHeaderFilterStrategy getSjmsHeaderFilterStrategy() {
        return sjmsHeaderFilterStrategy;
    }

    public void setSjmsHeaderFilterStrategy(
            SjmsHeaderFilterStrategy sjmsHeaderFilterStrategy) {
        this.sjmsHeaderFilterStrategy = sjmsHeaderFilterStrategy;
    }

    public JmsKeyFormatStrategy getJmsKeyFormatStrategy() {
        return jmsKeyFormatStrategy;
    }

    public void setJmsKeyFormatStrategy(
            JmsKeyFormatStrategy jmsKeyFormatStrategy) {
        this.jmsKeyFormatStrategy = jmsKeyFormatStrategy;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

}
