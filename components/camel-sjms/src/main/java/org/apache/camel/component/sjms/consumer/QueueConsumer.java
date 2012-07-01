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

    protected SjmsEndpoint getQueueEndpoint() {
        return (SjmsEndpoint)this.getEndpoint();
    }
    
    protected ConnectionPool getConnectionPool() {
        return getQueueEndpoint().getConnections();
    }
    
    protected SessionPool getSessionPool() {
        return getQueueEndpoint().getSessions();
    }

    /**
     * Gets the boolean value of transacted for this instance of QueueProducer.
     *
     * @return the transacted
     */
    public boolean isTransacted() {
        return getQueueEndpoint().isTransacted();
    }

    /**
     * Gets the boolean value of async for this instance of QueueProducer.
     *
     * @return the async
     */
    public boolean isAsync() {
        return getQueueEndpoint().isAsyncConsumer();
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
    public int getConsumerCount() {
        return getQueueEndpoint().getConsumerCount();
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
