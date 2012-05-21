/*
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

import java.util.Map;

import javax.jms.ConnectionFactory;

import org.apache.camel.Endpoint;
import org.apache.camel.component.sjms.jms.queue.QueueEndpoint;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.spi.HeaderFilterStrategyAware;

/**
 * Represents the component that manages {@link SimpleJmsEndpoint}.
 */
public class SjmsComponent extends DefaultComponent implements HeaderFilterStrategyAware {

    private ConnectionFactory connectionFactory;
    private SjmsComponentConfiguration configuration;
    private HeaderFilterStrategy headerFilterStrategy = new SjmsHeaderFilterStrategy();

    protected Endpoint createEndpoint(String uri, String remaining,
            Map<String, Object> parameters) throws Exception {
        Endpoint endpoint = null;
        if (uri.indexOf("://queue:") > -1) {
            endpoint = new QueueEndpoint(uri, this);
        } else {
//            endpoint = new TopicEndpoint(uri, this);
        }
        setProperties(endpoint, parameters);
        return endpoint;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    /**
     * Sets the ConnectionFactory value of connectionFactory for this instance
     * of SjmsComponent.
     * 
     * @param connectionFactory
     *            Sets ConnectionFactory, default is TODO add default
     */
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        if(getConfiguration().getConnectionFactory() == null)
            getConfiguration().setConnectionFactory(connectionFactory);
    }

    /**
     * Gets the ConnectionFactory value of connectionFactory for this instance
     * of SjmsComponent.
     * 
     * @return the connectionFactory
     */
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * Sets the SjmsComponentConfiguration value of configuration for this
     * instance of SjmsComponent.
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
     * instance of SjmsComponent.
     * 
     * @return the configuration
     */
    public SjmsComponentConfiguration getConfiguration() {
        if (configuration == null) {
            configuration = new SjmsComponentConfiguration();
        }
        return configuration;
    }

    @Override
    public HeaderFilterStrategy getHeaderFilterStrategy() {
        return this.headerFilterStrategy;
    }

    @Override
    public void setHeaderFilterStrategy(HeaderFilterStrategy headerFilterStrategy) {
        this.headerFilterStrategy = headerFilterStrategy;
    }
}
