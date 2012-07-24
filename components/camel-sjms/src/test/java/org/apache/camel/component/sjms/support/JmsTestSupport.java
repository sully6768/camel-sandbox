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
package org.apache.camel.component.sjms.support;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.test.junit4.CamelTestSupport;

/**
 * TODO Add Class documentation for JmsTestSupport
 *
 * @author sully6768
 */
public class JmsTestSupport extends CamelTestSupport {
	
	static{
		System.setProperty("org.apache.activemq.default.directory.prefix", "target/activemq/");
	}
	
    @Produce
    protected ProducerTemplate template;
    protected ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://broker?broker.useJmx=false");
    private Connection connection;
    private Session session;

    @Override
    protected void doPreSetup() throws Exception {
    	super.doPreSetup();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        setConnection(connectionFactory.createConnection());
        getConnection().start();
        setSession(getConnection().createSession(false, Session.AUTO_ACKNOWLEDGE));
    }

    @Override
    public void tearDown() throws Exception {
        if (getConnection() != null) {
            getConnection().stop();
        }
        if (getSession() != null) {
            getSession().close();
        }
        super.tearDown();
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
        SjmsComponent component = new SjmsComponent();
        component.setMaxConnections(1);
        component.setConnectionFactory(connectionFactory);
        camelContext.addComponent("sjms", component);
        return camelContext;
    }

	protected void sendTextMessage(final String queueName, final String expectedBody) throws JMSException {
		Session session = this.getSession();
	    Destination queue = session.createQueue(queueName);
	    MessageProducer producer = session.createProducer(queue);
	    TextMessage txtMessage = session.createTextMessage();
	    txtMessage.setText(expectedBody);
	    producer.send(txtMessage);
	}
    
    public void setSession(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

}
