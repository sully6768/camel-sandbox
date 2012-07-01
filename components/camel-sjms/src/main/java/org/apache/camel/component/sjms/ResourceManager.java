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

import javax.jms.ConnectionFactory;

import org.apache.camel.ExchangePattern;
import org.apache.camel.component.sjms.pool.ConnectionPool;
import org.apache.camel.component.sjms.pool.SessionPool;


/**
 * TODO Add Class documentation for ResourceManager
 *
 * @author sully6768
 */
public class ResourceManager {
    
    private ConnectionFactory connectionFactory;
    private int connectionCount = 1;
    private ConnectionPool connectionPool;
    private SessionPool sessionPool;
    private int sessionCount = 1;
    private boolean transacted = false;

    /**
     * TODO Add Constructor Javadoc
     *
     * @param connectionFactory
     * @param connectionCount
     * @param sessionCount
     * @param transacted
     */
    private ResourceManager(ConnectionFactory connectionFactory,
            int connectionCount, int sessionCount, boolean transacted) {
        super();
        this.connectionFactory = connectionFactory;
        this.connectionCount = connectionCount;
        this.sessionCount = sessionCount;
        this.transacted = transacted;
    }

    public SjmsMessageConsumer createMessageHandler(String destinationName, String messageSelector, ExchangePattern exchangePattern) {
        // if transacted create a new session, otherwise draw from the pool
        return null;
    }

    public SjmsMessageConsumer createMessageHandler(String destinationName, String messageSelector, String subscriptionName, ExchangePattern exchangePattern) {
        return null;
    }
    
    public SjmsMessageProducer createMessageProcessor(String destinationName, String messageSelector, ExchangePattern exchangePattern) {
        return null;
    }
}
