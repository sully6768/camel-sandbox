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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.RecipientList;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.util.ObjectHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Scott ES sully6768@yahoo.com
 *
 */
public class HttpProxyRecipientList {
	
	private static final Logger LOG = LoggerFactory.getLogger(HttpProxyRecipientList.class);

	private Properties properties;
	private Map<String, String> addressMap;
	
	public void init() {
		LOG.info("Initializing the RecipientListProcessor");
		LOG.info("Build the recipient list map");
		if(properties == null) {
			throw new RuntimeCamelException("The properties field has not been set");
		} else {
			addressMap = new HashMap<String, String>();
			for (Object keyObj : properties.keySet()) {
				String key = (String) keyObj;
				if(key.startsWith("proxy.address.")) {
					String value = properties.getProperty(key);
					addressMap.put(key, value);
				}
			}
		}
	}
	
	public void destroy() {
		LOG.info("Cleaning up the RecipientListProcessor");
		LOG.info("Destroy the recipient list map");
		addressMap.clear();
		addressMap = null;
	}

	@RecipientList
	public String routeList(
			@Header(value=Exchange.HTTP_URI) String uri, 
			@Header(value=Exchange.HTTP_URL) String url) throws Exception {
		String answer = null;
		uri = uri.substring(1).replace('/', '.');
		for (String key : addressMap.keySet()) {
			String propKey = "proxy.address." + uri;
			if (propKey.startsWith(key)) {
				answer = addressMap.get("proxy.address." + uri) + "?bridgeEndpoint=true";
			}			
		}
		if (ObjectHelper.isEmpty(answer)) {
			throw new Exception(url);
		}
		return answer;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public Properties getProperties() {
		return properties;
	}
}
