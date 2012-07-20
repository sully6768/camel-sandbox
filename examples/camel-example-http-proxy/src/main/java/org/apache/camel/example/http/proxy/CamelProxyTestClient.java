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

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;

/**
 * Class used to test a deployed test client
 *
 * @author sully6768
 */
public class CamelProxyTestClient {
	private CamelContext context;
	
	public CamelProxyTestClient() {
		try {
			context = new DefaultCamelContext();
			context.start();
		} catch (Exception e) {
			 e.printStackTrace();
		}
	}
	
	public void stop() throws Exception {
		context.stop();
	}
	
	public void testWsdlProxy() throws Exception {
		ProducerTemplate template = context.createProducerTemplate();
		String wsdl = template.requestBodyAndHeader("jetty:http://localhost:11111/cxf/camel-example-cxf-osgi/webservices/incident?enableJmx=true", "", Exchange.HTTP_QUERY, "wsdl", String.class);
		System.out.println("");
		System.out.println("WSDL: \n" + wsdl);
	}
	
	public void testBobRequestProxy() throws Exception {
		ProducerTemplate template = context.createProducerTemplate();
		StringBuilder sb = new StringBuilder();
		sb.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:rep=\"http://reportincident.example.camel.apache.org\">");
		sb.append("<soapenv:Header/>");
		sb.append("<soapenv:Body>");
		sb.append("<rep:inputReportIncident>");
		sb.append("   <incidentId></incidentId>");
		sb.append("   <incidentDate>2011-11-18</incidentDate>");
		sb.append("   <givenName>Bob</givenName>");
		sb.append("   <familyName>Smith</familyName>");
		sb.append("   <summary>Bla bla</summary>");
		sb.append("   <details>More bla</details>");
		sb.append("   <email>davsclaus@apache.org</email>");
		sb.append("   <phone>12345678</phone>");
		sb.append("</rep:inputReportIncident>");
		sb.append("</soapenv:Body>");
		sb.append("</soapenv:Envelope>");
		
		String response = template.requestBodyAndHeader("jetty:http://localhost:11111/cxf/camel-example-cxf-osgi/webservices/incident?enableJmx=true", sb.toString(), Exchange.HTTP_METHOD, "POST", String.class);
		System.out.println("");
		System.out.println("Response: \n" + response);
	}
	
	public void testClausRequestProxy() throws Exception {
		ProducerTemplate template = context.createProducerTemplate();
		StringBuilder sb = new StringBuilder();
		sb.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:rep=\"http://reportincident.example.camel.apache.org\">");
		sb.append("<soapenv:Header/>");
		sb.append("<soapenv:Body>");
		sb.append("<rep:inputReportIncident>");
		sb.append("   <incidentId></incidentId>");
		sb.append("   <incidentDate>2011-11-18</incidentDate>");
		sb.append("   <givenName>Claus</givenName>");
		sb.append("   <familyName>Isben</familyName>");
		sb.append("   <summary>Bla bla</summary>");
		sb.append("   <details>More bla</details>");
		sb.append("   <email>davsclaus@apache.org</email>");
		sb.append("   <phone>12345678</phone>");
		sb.append("</rep:inputReportIncident>");
		sb.append("</soapenv:Body>");
		sb.append("</soapenv:Envelope>");
		
		String response = template.requestBodyAndHeader("jetty:http://localhost:11111/cxf/camel-example-cxf-osgi/webservices/incident", sb.toString(), Exchange.HTTP_METHOD, "POST", String.class);
		System.out.println("");
		System.out.println("Response: \n" + response);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		CamelProxyTestClient cpc = new CamelProxyTestClient();
		cpc.testWsdlProxy();
		cpc.testBobRequestProxy();
		cpc.testClausRequestProxy();
		cpc.stop();
	}

}
