/*
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.camel.component.sjms.support;

import javax.jms.Connection;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.junit4.CamelTestSupport;

/**
 * TODO Add Class documentation for JmsTestSupport
 *
 * @author sully6768
 */
public class JmsTestSupport extends CamelTestSupport {

    @Produce
    protected ProducerTemplate template;
    protected ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://broker?broker.persistent=false&broker.useJmx=false");
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

    /**
     * Sets the Session value of session for this instance of JmsTestSupport.
     *
     * @param session Sets Session, default is TODO add default
     */
    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * Gets the Session value of session for this instance of JmsTestSupport.
     *
     * @return the session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Sets the Connection value of connection for this instance of JmsTestSupport.
     *
     * @param connection Sets Connection, default is TODO add default
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * Gets the Connection value of connection for this instance of JmsTestSupport.
     *
     * @return the connection
     */
    public Connection getConnection() {
        return connection;
    }

}
