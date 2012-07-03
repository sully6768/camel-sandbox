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
package org.apache.camel.component.sjms.consumer;

import java.security.SecureRandom;
import java.util.UUID;

import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.component.sjms.SjmsEndpoint;
import org.apache.camel.component.sjms.pool.ConnectionPool;
import org.apache.camel.component.sjms.pool.SessionPool;
import org.apache.camel.impl.DefaultConsumer;

/**
 * TODO Add Class documentation for QueueConsumer
 *
 */
public abstract class QueueConsumer extends DefaultConsumer {

    /**
     * TODO Add Constructor Javadoc
     *
     * @param endpoint
     * @param processor
     */
    public QueueConsumer(Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    protected SjmsEndpoint getSjmsEndpoint() {
        return (SjmsEndpoint)this.getEndpoint();
    }
    
    protected ConnectionPool getConnectionPool() {
        return getSjmsEndpoint().getConnections();
    }
    
    protected SessionPool getSessionPool() {
        return getSjmsEndpoint().getSessions();
    }

    public boolean isEndpointTransacted() {
        return getSjmsEndpoint().isTransacted();
    }

    public boolean isSynchronous() {
        return getSjmsEndpoint().isSynchronous();
    }

    public String getDestinationName() {
        return getSjmsEndpoint().getDestinationName();
    }

    public int getConsumerCount() {
        return getSjmsEndpoint().getConsumerCount();
    }
    
    public boolean isTopic() {
        return getSjmsEndpoint().isTopic();
    }

    /**
     * @return
     */
    protected String createId() {
        String answer = null;
        SecureRandom ng = new SecureRandom();
        UUID uuid = new UUID(ng.nextLong(), ng.nextLong());
        answer = uuid.toString();
        return answer;
    }
    
}
