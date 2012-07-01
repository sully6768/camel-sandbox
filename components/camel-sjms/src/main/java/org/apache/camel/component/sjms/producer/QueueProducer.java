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

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;

import org.apache.camel.component.sjms.SjmsEndpoint;
import org.apache.camel.component.sjms.SjmsProducer;

/**
 * TODO Add Class documentation for QueueConsumer
 *
 */
public class QueueProducer extends SjmsProducer {
    
    public QueueProducer(SjmsEndpoint endpoint) {
        super(endpoint);
    }
    
    public MessageProducerModel doCreateProducerModel() throws Exception {
        QueueConnection conn = (QueueConnection) getConnectionPool().borrowObject();
        QueueSession queueSession = (QueueSession) conn.createSession(false, getAcknowledgeMode());
        Queue myQueue = queueSession.createQueue(getDestinationName());
        QueueSender queueSender = queueSession.createSender(myQueue);
        getConnectionPool().returnObject(conn);
        return new MessageProducerModel(queueSession, queueSender);
    }
}
