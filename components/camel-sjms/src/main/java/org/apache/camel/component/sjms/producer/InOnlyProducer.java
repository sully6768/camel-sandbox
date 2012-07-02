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

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.sjms.JmsMessageHelper;
import org.apache.camel.component.sjms.SjmsEndpoint;
import org.apache.camel.component.sjms.SjmsProducer;
import org.apache.camel.component.sjms.jms.JmsObjectFactory;

/**
 * TODO Add Class documentation for QueueConsumer
 *
 */
public class InOnlyProducer extends SjmsProducer {
    
    public InOnlyProducer(SjmsEndpoint endpoint) {
        super(endpoint);
    }
    
    public MessageProducerModel doCreateProducerModel() throws Exception {
        Connection conn = getConnectionPool().borrowObject();
        Session session = null;
        if (isEndpointTransacted()) {
            session = conn.createSession(true, getAcknowledgeMode());
        } else {
            session = conn.createSession(false, getAcknowledgeMode());
        }
        MessageProducer messageProducer = null;
        if(isTopic()) {
            messageProducer = JmsObjectFactory.createTopicProducer(session, getDestinationName());
        } else {
            messageProducer = JmsObjectFactory.createQueueProducer(session, getDestinationName());
        }
        getConnectionPool().returnObject(conn);
        return new MessageProducerModel(session, messageProducer);
    }
    
    @Override
    public boolean process(final Exchange exchange, final AsyncCallback callback) {
        boolean syncProcessing = false;
        if(log.isDebugEnabled()) {
            log.debug("Processing InOnly Exchange id:{}", exchange.getExchangeId());
        }
        try {
            if( ! isSyncronous()) {
                if(log.isDebugEnabled()) {
                    log.debug("Sending message asynchronously for Exchange id:{}", exchange.getExchangeId());
                }
                getExecutor().execute(new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            sendMessage(exchange);
                        } catch (Exception e) {
                            throw new RuntimeCamelException(e);
                        }
                    }
                });
                
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("Sending message synchronously for Exchange id:{}", exchange.getExchangeId());
                }
                syncProcessing = true;
                sendMessage(exchange);
            }
        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.debug("Processing InOnly Exchange id:{}", exchange.getExchangeId() + " - Failed");
            }
            exchange.setException(e);
        }
        callback.done(syncProcessing);
        if(log.isDebugEnabled()) {
            log.debug("Processing InOnly Exchange id:{}", exchange.getExchangeId() + " - SUCCESS");
        }
        return syncProcessing;
    }
    

    private void sendMessage(Exchange exchange) throws Exception {
        if (getProducers() != null) {
            MessageProducerModel model = getProducers().borrowObject();

            if (isEndpointTransacted()) {
                exchange.addOnCompletion(new ProducerSynchronization(model.getSession()));
            }
            
            Message message = JmsMessageHelper.createMessage(exchange, model.getSession());
            model.getMessageProducer().send(message);
        }
    }
}
