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
package org.apache.camel.component.sjms.consumer;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.component.sjms.jms.JmsMessageHeaderType;
import org.apache.camel.test.junit4.CamelTestSupport;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactedInOnlyConsumerTest extends CamelTestSupport {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    
    public TransactedInOnlyConsumerTest() {
    	enableJMX();
	}
    
    @Override
    protected boolean useJmx() {
    	return true;
    }

    @Test
    public void testTransactedInOnlyConsumerExchangeFailure() throws Exception {
        // We should see the World message twice, once for the exception
        getMockEndpoint("mock:test1.mock.before").expectedBodiesReceived("World","World");
        getMockEndpoint("mock:test1.mock.after").expectedBodiesReceived("Hello World");
        
        template.sendBody("sjms:queue:test1.queue", "World");
        
        getMockEndpoint("mock:test1.mock.before").assertIsSatisfied();
        getMockEndpoint("mock:test1.mock.after").assertIsSatisfied();

    }

    @Test
    public void testTransactedInOnlyConsumerRuntimeException() throws Exception {
        // We should see the World message twice, once for the exception
        getMockEndpoint("mock:test2.mock.before").expectedBodiesReceived("World","World");
        getMockEndpoint("mock:test2.mock.after").expectedBodiesReceived("Hello World");
        
        template.sendBody("sjms:queue:test2.queue", "World");
        
        getMockEndpoint("mock:test2.mock.before").assertIsSatisfied();
        getMockEndpoint("mock:test2.mock.after").assertIsSatisfied();
    }

    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                "vm://broker?broker.persistent=false&broker.useJmx=true");
        SjmsComponent component = new SjmsComponent();
        component.setConnectionFactory(connectionFactory);
        camelContext.addComponent("sjms", component);

        return camelContext;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("sjms:queue:test1.queue?transacted=true")
                    .to("log:test1.before")
                    .to("mock:test1.mock.before")
                    .process(new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            LOGGER.info("Begin processing Exchange ID: {}", exchange.getExchangeId());
                            if (! exchange.getIn().getHeader(JmsMessageHeaderType.JMSRedelivered.toString(), String.class).equalsIgnoreCase("true")) {
                                LOGGER.info("Exchange does not have a retry message.  Set the exception and allow the retry.");
                                exchange.setException(new RuntimeCamelException("Creating Failure"));
                            } else {
                                LOGGER.info("Exchange has retry header.  Continue processing the message.");
                            }
                        }
                    })
                    .transform(body().prepend("Hello "))
                    .to("log:test1.after?showAll=true", "mock:test1.mock.after");
                
                from("sjms:queue:test2.queue?transacted=true")
                    .to("log:test2.before")
                    .to("mock:test2.mock.before")
                    .process(new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            LOGGER.info("Begin processing Exchange ID: {}", exchange.getExchangeId());
                            if (! exchange.getIn().getHeader(JmsMessageHeaderType.JMSRedelivered.toString(), String.class).equalsIgnoreCase("true")) {
                                LOGGER.info("Exchange does not have a retry message.  Throw the exception to verify we handle the retry.");
                                throw new RuntimeCamelException("Creating Failure");
                            } else {
                                LOGGER.info("Exchange has retry header.  Continue processing the message.");
                            }
                        }
                    })
                    .transform(body().prepend("Hello "))
                    .to("log:test2.after?showAll=true", "mock:test2.mock.after");
            }
        };
    }
}
