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
package org.apache.camel.component.sjms.jms.queue;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ResolveEndpointFailedException;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.component.sjms.SjmsComponentConfiguration;
import org.apache.camel.test.junit4.CamelTestSupport;

import org.junit.Test;

public class QueueEndpointTest extends CamelTestSupport {
    
    public QueueEndpointTest() {
    	enableJMX();
	}
    
    @Override
    protected boolean useJmx() {
    	return true;
    }

    @Test
    public void testQueueEndpoint() throws Exception {
        Endpoint sjms = context.getEndpoint("sjms:queue:test");
        assertNotNull(sjms);
        assertTrue(sjms instanceof QueueEndpoint);
    }

    @Test
    public void testSetTransacted() throws Exception {
        Endpoint endpoint = context.getEndpoint("sjms:queue:test?transacted=true");
        assertNotNull(endpoint);
        assertTrue(endpoint instanceof QueueEndpoint);
        QueueEndpoint qe = (QueueEndpoint) endpoint;
        assertTrue(qe.isTransacted());
    }

    @Test
    public void testAsyncConsumer() throws Exception {
        Endpoint endpoint = context.getEndpoint("sjms:queue:test?asyncConsumer=true");
        assertNotNull(endpoint);
        assertTrue(endpoint instanceof QueueEndpoint);
        QueueEndpoint qe = (QueueEndpoint) endpoint;
        assertTrue(qe.isAsyncConsumer());
    }

    @Test
    public void testAsyncProducer() throws Exception {
        Endpoint endpoint = context.getEndpoint("sjms:queue:test?asyncProducer=true");
        assertNotNull(endpoint);
        assertTrue(endpoint instanceof QueueEndpoint);
        QueueEndpoint qe = (QueueEndpoint) endpoint;
        assertTrue(qe.isAsyncProducer());
    }

    @Test
    public void testNamedReplyTo() throws Exception {
        String namedReplyTo = "reply.to.queue";
        Endpoint endpoint = context.getEndpoint("sjms:queue:test?namedReplyTo=" + namedReplyTo);
        assertNotNull(endpoint);
        assertTrue(endpoint instanceof QueueEndpoint);
        QueueEndpoint qe = (QueueEndpoint) endpoint;
        assertEquals(qe.getNamedReplyTo(), namedReplyTo);
        assertEquals(qe.createExchange().getPattern(), ExchangePattern.InOut);
    }

    @Test
    public void testRobustInOnlyMessageExchangePattern() throws Exception {
        try {
            Endpoint sjms = context.getEndpoint("sjms:queue:test?messageExchangePattern=" + ExchangePattern.RobustInOnly);
            assertNotNull(sjms);
            assertTrue(sjms.createExchange().getPattern().equals(ExchangePattern.RobustInOnly));
        } catch (Exception e) {
            fail("Exception thrown: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void testInOnlyExchangePattern() throws Exception {
        try {
            Endpoint sjms = context.getEndpoint("sjms:queue:test?messageExchangePattern=" + ExchangePattern.InOnly);
            assertNotNull(sjms);
            assertTrue(sjms.createExchange().getPattern().equals(ExchangePattern.InOnly));
        } catch (Exception e) {
            fail("Exception thrown: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void testInOutExchangePattern() throws Exception {
        try {
            Endpoint sjms = context.getEndpoint("sjms:queue:test?messageExchangePattern=" + ExchangePattern.InOut);
            assertNotNull(sjms);
            assertTrue(sjms.createExchange().getPattern().equals(ExchangePattern.InOut));
        } catch (Exception e) {
            fail("Exception thrown: " + e.getLocalizedMessage());
        }
    }

    @Test(expected=ResolveEndpointFailedException.class)
    public void testUnsupportedMessageExchangePattern() throws Exception {
        context.getEndpoint("sjms:queue:test2?messageExchangePattern=" + ExchangePattern.OutOnly);
    }

    @Test
    public void testNamedReplyToAndMEPMatch() throws Exception {
        String namedReplyTo = "reply.to.queue";
        Endpoint endpoint = context.getEndpoint("sjms:queue:test?namedReplyTo="+namedReplyTo+"&messageExchangePattern=" + ExchangePattern.InOut);
        assertNotNull(endpoint);
        assertTrue(endpoint instanceof QueueEndpoint);
        QueueEndpoint qe = (QueueEndpoint) endpoint;
        assertEquals(qe.getNamedReplyTo(), namedReplyTo);
        assertEquals(qe.createExchange().getPattern(), ExchangePattern.InOut);
    }

    @Test(expected=Exception.class)
    public void testNamedReplyToAndMEPMismatch() throws Exception {
        context.getEndpoint("sjms:queue:test?namedReplyTo=reply.to.queue&messageExchangePattern=" + ExchangePattern.InOnly);
    }

    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                "vm://broker?broker.persistent=false&broker.useJmx=false");
        SjmsComponentConfiguration config = new SjmsComponentConfiguration();
        config.setMaxConnections(3);
        config.setMaxSessions(5);
        config.setMaxConsumers(10);
        config.setMaxProducers(2);
        SjmsComponent component = new SjmsComponent();
        component.setConfiguration(config);
        component.setConnectionFactory(connectionFactory);
        camelContext.addComponent("sjms", component);

        return camelContext;
    }
}
