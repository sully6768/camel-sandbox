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
package org.apache.camel.example.http.proxy;

import java.util.concurrent.TimeUnit;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelSpringTestSupport;

import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Camel route unit test.
 * 
 */
public class CamelMockedRouteTest extends CamelSpringTestSupport {

	
	/* (non-Javadoc)
	 * @see org.apache.camel.test.junit4.CamelSpringTestSupport#createApplicationContext()
	 */
	@Override
	protected AbstractApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext("META-INF/spring/camel-context.xml", "test-properties-context.xml");
	}

	@Test
	public void testPingRoute() throws Exception {
		MockEndpoint mockPing = getMockEndpoint("mock:ping");
		mockPing.expectedMessageCount(1);
		mockPing.expectedBodiesReceived("Hi from Camel");
		
		String response = template.requestBody("jetty:http://localhost:11111/ping", "Hi from Camel", String.class);
		assertEquals("Hi from PING", response);
		
		mockPing.await(3, TimeUnit.SECONDS);
		mockPing.assertIsSatisfied();
	}

	@Test
	public void testFooRoute() throws Exception {
		MockEndpoint mockPing = getMockEndpoint("mock:foo");
		mockPing.expectedMessageCount(1);
		mockPing.expectedBodiesReceived("Hi from Camel");
		
		String response = template.requestBody("jetty:http://localhost:11111/foo", "Hi from Camel", String.class);
		assertEquals("Hi from FOO", response);
		
		mockPing.await(3, TimeUnit.SECONDS);
		mockPing.assertIsSatisfied();
	}

	@Test
	public void testBarRoute() throws Exception {
		MockEndpoint mockPing = getMockEndpoint("mock:bar");
		mockPing.expectedMessageCount(1);
		mockPing.expectedBodiesReceived("Hi from Camel");
		
		String response = template.requestBody("jetty:http://localhost:11111/bar", "Hi from Camel", String.class);
		assertEquals("Hi from BAR", response);
		
		mockPing.await(3, TimeUnit.SECONDS);
		mockPing.assertIsSatisfied();
	}

	@Test
	public void testFooBarRoute() throws Exception {
		MockEndpoint mockPing = getMockEndpoint("mock:foo.bar");
		mockPing.expectedMessageCount(1);
		mockPing.expectedBodiesReceived("Hi from Camel");
		
		String response = template.requestBody("jetty:http://localhost:11111/foo/bar", "Hi from Camel", String.class);
		assertEquals("Hi from FOOBAR", response);
		
		mockPing.await(3, TimeUnit.SECONDS);
		mockPing.assertIsSatisfied();
	}

	@Test
	public void testNoSuchRoute() throws Exception {
		String response = template.requestBody("jetty:http://localhost:11111/nosuchroute", "Hi from Camel", String.class);
		assertEquals("Unsupported Address: http://localhost:11111/nosuchroute - no services found at this address.", response);
	}
	
	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		// TODO Auto-generated method stub
		return new RouteBuilder() {
			
			@Override
			public void configure() throws Exception {
				from("jetty:http://localhost:22222/ping")
						.log("Hi from the PING ROUTE")
						.to("mock:ping")
						.transform(constant("Hi from PING"));
				
				from("jetty:http://localhost:33333/foo")
					.log("Hi from the FOO ROUTE")
					.to("mock:foo")
					.transform(constant("Hi from FOO"));
				
				from("jetty:http://localhost:44444/bar")
					.log("Hi from the BAR ROUTE")
					.to("mock:bar")
					.transform(constant("Hi from BAR"));
				
				from("jetty:http://localhost:55555/foo/bar")
					.log("Hi from the FOOBAR ROUTE")
					.to("mock:foo.bar")
					.transform(constant("Hi from FOOBAR"));
			}
		};
	}
}
