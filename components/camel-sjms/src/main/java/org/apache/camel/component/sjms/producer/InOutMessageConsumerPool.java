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

import java.util.concurrent.Exchanger;

import org.apache.camel.component.sjms.pool.ConnectionPool;
import org.apache.camel.component.sjms.pool.ObjectPool;
import org.apache.camel.component.sjms.pool.SessionPool;

/**
 * TODO Add Class documentation for InOutMessageProducerPool
 *
 * @author sully6768
 */
public class InOutMessageConsumerPool extends ObjectPool<InOutMessageConsumer> {
    private final ObjectPool<?> connectionPool;
    private final String destinationName;
    private Exchanger<Object> exchanger;
    
    public InOutMessageConsumerPool(ConnectionPool connectionPool, String destinationName, int consumerCount) {
        super(consumerCount);
        this.connectionPool = connectionPool;
        this.destinationName = destinationName;
    }
    
    public InOutMessageConsumerPool(SessionPool connectionPool, String destinationName, int consumerCount, Exchanger<Object> exchanger) {
        super(consumerCount);
        this.connectionPool = connectionPool;
        this.destinationName = destinationName;
    }

    @Override
    protected InOutMessageConsumer createObject() throws Exception {
        InOutMessageConsumer iomp = new InOutMessageConsumer();
        if(connectionPool != null) {
            return  iomp.createMessageConsumer((ConnectionPool) connectionPool, destinationName);
        } else {
            return iomp.createMessageConsumerListener((SessionPool)connectionPool, destinationName, exchanger);
        }
    }
    
    @Override
    protected void destroyObject(InOutMessageConsumer producer) throws Exception {
        producer.destroyMessageConsumer();
    }
}
