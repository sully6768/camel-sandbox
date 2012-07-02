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
package org.apache.camel.component.sjms.producer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.component.sjms.SjmsComponentConfiguration;
import org.apache.camel.component.sjms.jms.JmsObjectFactory;
import org.apache.camel.component.sjms.support.JmsTestSupport;

import org.junit.Test;

public class InOutQueueProducerLoadTest extends JmsTestSupport {
    
    private static final String TEST_DESTINATION_NAME = "in.out.queue.producer.test";
    private MessageConsumer mc;
    public InOutQueueProducerLoadTest() {
	}
    
    @Override
    protected boolean useJmx() {
    	return false;
    }
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mc = JmsObjectFactory.createQueueConsumer(getSession(), TEST_DESTINATION_NAME + ".request");
        mc.setMessageListener(new MyMessageListener());
    }
    
    @Override
    public void tearDown() throws Exception {
        mc.close();
        super.tearDown();
    }

    @Test
    public void testInOutQueueProducer() throws Exception {
//        final String requestText = "Hello World!";
//        final String responseText = "How are you";

//        ConcurrentHashMap<String, Future<String>> correlationMap = new ConcurrentHashMap<String, Future<String>>();

        ExecutorService executor = Executors.newFixedThreadPool(5);


        for (int i = 1; i <= 5000; i++) {
            final int tempI = i;
            Runnable worker = new Runnable() {
                
                @Override
                public void run() {
                    try {
                        final String requestText = "Message " + tempI;
                        final String responseText = "Response Message " + tempI;
//                    Future<String> future = template.asyncRequestBody("direct:start", requestText, String.class);
//                    String response = future.get();
                        String response = template.requestBody("direct:start", requestText, String.class);
                        assertNotNull(response);
                        assertEquals(responseText, response);
                    } catch (Exception e) {
                        log.error("TODO Auto-generated catch block", e);
                    }                   
                }
            };
            executor.execute(worker);
        }
        while(context.getInflightRepository().size() > 0){
            
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
//
        }
        
//        for (int i = 0; i < 1000; i++) {
//            assertNotNull(mc);
//        }
        mc.close();
        
//        for (String responseText : correlationMap.keySet()) {
//            Future<String> future = correlationMap.get(responseText);
//            String response = future.get(5000, TimeUnit.MILLISECONDS);
//            assertNotNull(response);
//            assertEquals(responseText, response);
//        }
    }
    
    /*
     * @see org.apache.camel.test.junit4.CamelTestSupport#createCamelContext()
     *
     * @return
     * @throws Exception
     */
    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        SjmsComponentConfiguration config = new SjmsComponentConfiguration();
        config.setMaxConnections(1);
        SjmsComponent component = new SjmsComponent();
        component.setConfiguration(config);
        component.setConnectionFactory(connectionFactory);
        camelContext.addComponent("sjms", component);
        return camelContext;
    }

    /*
     * @see org.apache.camel.test.junit4.CamelTestSupport#createRouteBuilder()
     *
     * @return
     * @throws Exception
     */
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct:start")
                    .to("log:" + TEST_DESTINATION_NAME + ".in.log?showBody=true")
                    .inOut("sjms:queue:" + TEST_DESTINATION_NAME + ".request" + "?namedReplyTo=" + TEST_DESTINATION_NAME + ".response&consumerCount=10&producerCount=20&synchronous=false")
                    .to("log:" + TEST_DESTINATION_NAME + ".out.log?showBody=true");
            }
        };
    }
    
    protected class MyMessageListener implements MessageListener {
        public MyMessageListener() {
            super();
        }
        
        @Override
        public void onMessage(Message message) {
            try {
                TextMessage request = (TextMessage) message;
                String text = request.getText();
                
                TextMessage response = getSession().createTextMessage();
                response.setText("Response " + text);
                response.setJMSCorrelationID(request.getJMSCorrelationID());
                MessageProducer mp = getSession().createProducer(message.getJMSReplyTo());
                mp.send(response);
                mp.close();
            } catch (JMSException e) {
                fail(e.getLocalizedMessage());
            }
        }
    }
}
