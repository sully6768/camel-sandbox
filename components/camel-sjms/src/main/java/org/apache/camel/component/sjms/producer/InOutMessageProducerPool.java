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
package org.apache.camel.component.sjms.producer;

import org.apache.camel.component.sjms.pool.ConnectionPool;
import org.apache.camel.component.sjms.pool.ObjectPool;

/**
 * TODO Add Class documentation for InOutMessageProducerPool
 *
 * @author sully6768
 */
public class InOutMessageProducerPool extends ObjectPool<InOutMessageProducer> {
    private final ConnectionPool connectionPool;
    private final String destinationName;
    public InOutMessageProducerPool(ConnectionPool connectionPool, String destinationName, int producerCount) {
        super(producerCount);
        this.connectionPool = connectionPool;
        this.destinationName = destinationName;
    }

    @Override
    protected InOutMessageProducer createObject() throws Exception {
        InOutMessageProducer iomp = new InOutMessageProducer();
        return iomp.createMessageProducer(connectionPool, destinationName);
    }
    
    @Override
    protected void destroyObject(InOutMessageProducer producer) throws Exception {
        producer.destroyMessageProducer();
    }
}
