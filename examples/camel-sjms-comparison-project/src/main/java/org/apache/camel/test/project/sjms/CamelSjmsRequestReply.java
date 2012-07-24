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
package org.apache.camel.test.project.sjms;

import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StopWatch;

/**
 * An example class for demonstrating some of the basics behind Camel. This
 * example sends some text messages on to a JMS Queue, consumes them and
 * persists them to disk
 */
public final class CamelSjmsRequestReply {

    public static void main(String args[]) throws Exception {
    	new ClassPathXmlApplicationContext("camel-sjms-consumer.xml", CamelSjmsRequestReply.class);
    	ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("camel-sjms-producer.xml", CamelSjmsRequestReply.class);
    	CamelContext camel = context.getBean(CamelContext.class);
    	ProducerTemplate template = camel.createProducerTemplate();

    	for (int i = 0; i < 1000000; i++) {
			testBatchOfMessages(template, 1000);
		}
    }

    private static void testBatchOfMessages(ProducerTemplate template, int number){
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for (int i = 0; i < number; i++) {
			template.requestBody("direct:invokeJmsCamelQueue", "Test Message");
		}
		stopWatch.stop();
		System.out.println("Exchanged " + number + "  messages in " + stopWatch.getTotalTimeMillis() + " millis");
	}

    public static class MyService{
    	public String echo(Message message){
    		return (String) message.getBody();
    	}
    }
}
