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
package org.apache.camel.component.sjms;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;

import org.junit.Test;

public class SimpleJmsComponentTest extends CamelTestSupport {

//    @Produce(uri = "direct:start")
//    protected ProducerTemplate template;

    @Test
    public void testHelloWorld() throws Exception {
        SjmsComponent component = (SjmsComponent) this.context.getComponent("sjms");
        assertNotNull(component);
//        String expectedBody = "Hello World!";
//        MockEndpoint mock = getMockEndpoint("mock:result");
//        mock.expectedMinimumMessageCount(1);
//
//        mock.expectedBodiesReceived("Hello World! How are you?");
//        template.sendBody(expectedBody);
//
//        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {

                ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                        "vm://broker?broker.persistent=false");
                SjmsComponent component = new SjmsComponent();
                component.setConnectionFactory(connectionFactory);
                getContext().addComponent("sjms", component);
                
                from("sjms:queue:test.foo.in")
                    .to("sjms:queue:test.foo.out");
                
//                from("sjms:queue:test.foo")
//                    .process(new Processor() {
//                        
//                        @Override
//                        public void process(Exchange exchange) throws Exception {
//                            String body = (String) exchange.getIn().getBody();
//                            body += " How are you?";
//                            exchange.getIn().setBody(body);
//                            
//                        }
//                    })
//                    .to("mock:result");
            }
        };
    }
}
