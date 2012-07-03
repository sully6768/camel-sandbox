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

import java.security.SecureRandom;
import java.util.UUID;

import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.component.sjms.pool.ConnectionPool;
import org.apache.camel.component.sjms.pool.SessionPool;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.util.ObjectHelper;

/**
 * TODO Add Class documentation for SjmsConsumer
 *
 * @author sully6768
 */
public class SjmsConsumer extends DefaultConsumer {


    public SjmsConsumer(Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
        // TODO Auto-generated constructor stub
    }

    protected SjmsEndpoint getSjmsEndpoint() {
        return (SjmsEndpoint)this.getEndpoint();
    }

    public boolean isDurableSubscription() {
        return  ObjectHelper.isNotEmpty(getSjmsEndpoint().isPersistent());
    }

    public String getDurableSubscription() {
        return  getSjmsEndpoint().getDurableSubscription();
    }
    
    protected ConnectionPool getConnectionPool() {
        return getSjmsEndpoint().getConnections();
    }
    
    protected SessionPool getSessionPool() {
        return getSjmsEndpoint().getSessions();
    }

    public boolean isTransacted() {
        return getSjmsEndpoint().isTransacted();
    }

    public boolean isAsync() {
        return getSjmsEndpoint().isSynchronous();
    }

    public String getReplyTo() {
        return getSjmsEndpoint().getNamedReplyTo();
    }

    public String getDestinationName() {
        return getSjmsEndpoint().getDestinationName();
    }

    public int getConsumerCount() {
        return getSjmsEndpoint().getConsumerCount();
    }

    protected String createId() {
        String answer = null;
        SecureRandom ng = new SecureRandom();
        UUID uuid = new UUID(ng.nextLong(), ng.nextLong());
        answer = uuid.toString();
        return answer;
    }
}
