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
package org.apache.camel.component.sjms.consumer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.component.sjms.jms.JmsMessageHelper;
import org.apache.camel.component.sjms.jms.JmsObjectFactory;
import org.apache.camel.spi.Synchronization;
import org.apache.camel.util.AsyncProcessorHelper;
import org.apache.camel.util.ObjectHelper;

/**
 * TODO Add Class documentation for DefaultMessageHandler
 * 
 * @author sully6768
 */
public class InOutMessageHandler extends DefaultMessageHandler {
	
	private Map<String, MessageProducer> producerCache = new ConcurrentHashMap<String, MessageProducer>();

    /**
     * TODO Add Constructor Javadoc
     * 
     * @param endpoint
     * @param processor
     */
    public InOutMessageHandler(Endpoint endpoint, AtomicBoolean stopped,
            ExecutorService executor) {
        this(endpoint, stopped, executor, null);
    }

    /**
     * TODO Add Constructor Javadoc
     * 
     * @param stopped
     * @param synchronization
     */
    public InOutMessageHandler(Endpoint endpoint, AtomicBoolean stopped,
            ExecutorService executor, Synchronization synchronization) {
        super(endpoint, stopped, executor, synchronization);
    }
    
    /**
     * @param message
     */
    @Override
    public void doHandleMessage(final Exchange exchange) {
        if (log.isDebugEnabled()) {
            log.debug("SjmsMessageConsumer invoked for Exchange id:{} ",
                    exchange.getExchangeId());
        }
        try {
            MessageProducer messageProducer = null;
        	if (isStarted()) {
            	if (messageProducer == null) {
            		Object obj = exchange.getIn().getHeader("JMSReplyTo");
                    if (obj != null){
    					Destination replyTo = null;
    					if (isDestination(obj)) {
    						replyTo = (Destination) obj;
    					} else if (obj instanceof String) {
    						replyTo = JmsObjectFactory.createDestination(getSession(), (String) obj, isTopic());
    					} else {
    						throw new Exception(
    								"The value of JMSReplyTo must be a valid Destination or String.  Value provided: "
    										+ obj);
    					}
    					if (isTemporaryDestination(replyTo)) {
    						messageProducer = getSession().createProducer(replyTo);
    					} else {
    						String destinationName = getDestinationName(replyTo);
    						if (producerCache.containsKey(destinationName)) {
    							messageProducer = producerCache.get(destinationName);
    						} else {
    							messageProducer = getSession()
    									.createProducer(replyTo);
    							producerCache.put(destinationName, messageProducer);
    						}
    					}
                    }	
            	}
            	
                
                MessageHanderAsyncCallback callback = new MessageHanderAsyncCallback(
                        exchange, messageProducer);
                if (exchange.isFailed()) {
                    return;
                } else {
                    if (isTransacted() || isSynchronous()) {
                        // must process synchronous if transacted or configured to
                        // do so
                        if (log.isTraceEnabled()) {
                            log.trace("Processing Exchange id:{} synchronously",
                                    exchange.getExchangeId());
                        }
                        try {
                            AsyncProcessorHelper.process(getProcessor(), exchange);
                        } catch (Exception e) {
                            exchange.setException(e);
                        } finally {
                            callback.done(true);
                        }
                    } else {
                        // process asynchronous using the async routing engine
                        if (log.isTraceEnabled()) {
                            log.trace("Processing Exchange id:{} asynchronously",
                                    exchange.getExchangeId());
                        }
                        boolean sync = AsyncProcessorHelper.process(getProcessor(),
                                exchange, callback);
                        if (!sync) {
                            // will be done async so return now
                            return;
                        }
                    }
                }
            } else {
                log.warn(
                        "SjmsMessageConsumer invoked while stopped.  Exchange id:{} will not be processed.",
                        exchange.getExchangeId());
            }
        } catch(Exception e) {
        	
        }

        if (log.isDebugEnabled()) {
            log.debug("SjmsMessageConsumer invoked for Exchange id:{} ",
                    exchange.getExchangeId());
        }
    }
    
    @Override
    public void close() {
    	for (String key : producerCache.keySet()) {
			MessageProducer mp = producerCache.get(key);
			try {
				mp.close();
			} catch (JMSException e) {
				ObjectHelper.wrapRuntimeCamelException(e);
			}
		}
    	producerCache.clear();
    	
//    	if (messageProducer != null) {
//    		try {
//				messageProducer.close();
//			} catch (JMSException e) {
//				ObjectHelper.wrapRuntimeCamelException(e);
//			} finally {
//				messageProducer = null;
//			}
//    	}
    }
    
    private boolean isDestination(Object object) {
    	return (object instanceof Destination);
    }
    
    private boolean isTemporaryDestination(Destination destination) {
    	return (destination instanceof TemporaryQueue || destination instanceof TemporaryTopic);
    }
    
    private String getDestinationName(Destination destination) throws Exception {
    	String answer = null;
    	if (destination instanceof Queue) {
    		answer = ((Queue)destination).getQueueName();
    	} else if (destination instanceof Topic) {
    		answer = ((Topic)destination).getTopicName();
    	}
    		
    	return answer;
    }

    protected class MessageHanderAsyncCallback implements AsyncCallback {

        private Exchange exchange;
        private MessageProducer localProducer;

        /**
         * TODO Add Constructor Javadoc
         * 
         * @param xid
         * @param exchange
         */
        public MessageHanderAsyncCallback(Exchange exchange, MessageProducer localProducer) {
            super();
            this.exchange = exchange;
            this.localProducer = localProducer;
        }

        @Override
        public void done(boolean sync) {

            try {
                Message response = JmsMessageHelper.createMessage(exchange,
                        getSession(), true);
                response.setJMSCorrelationID(exchange.getIn().getHeader(
                        "JMSCorrelationID", String.class)); 
                localProducer.send(response);
            } catch (Exception e) {
            	exchange.setException(e);
            }
        }
    }
}
