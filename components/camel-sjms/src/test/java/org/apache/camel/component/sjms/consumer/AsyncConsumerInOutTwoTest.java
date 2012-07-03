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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.component.sjms.support.MyAsyncComponent;
import org.apache.camel.test.junit4.CamelTestSupport;

import org.junit.Test;

/**
 *
 */
public class AsyncConsumerInOutTwoTest extends CamelTestSupport {

    @Test
    public void testAsyncJmsConsumer() throws Exception {
        String out = template.requestBody("sjms:queue:start", "Hello World", String.class);
        assertEquals("Bye World", out);
    }

    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();

        camelContext.addComponent("async", new MyAsyncComponent());

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                "vm://broker?broker.persistent=false");
        SjmsComponent component = new SjmsComponent();
        component.setConnectionFactory(connectionFactory);
        camelContext.addComponent("sjms", component);

        return camelContext;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // enable async in only mode on the consumer
                from("sjms:queue:start?synchronous=false&?exchangePattern=InOut")
                    .to("async:camel?delay=2000")
                    .transform(constant("Bye World"));
            }
        };
    }
}
