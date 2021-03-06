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
package org.apache.camel.component.sjms.tx;

import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.camel.Exchange;
import org.apache.camel.spi.Synchronization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Add Class documentation for SessionTransactionSynchronization
 *
 * @author sully6768
 */
public class SessionTransactionSynchronization implements Synchronization {
    private Logger log = LoggerFactory.getLogger(getClass());
    private Session session;
    
    public SessionTransactionSynchronization(Session session) {
        this.session = session;
    }
    
    /*
     * @see org.apache.camel.spi.Synchronization#onFailure(org.apache.camel.Exchange)
     *
     * @param exchange
     */
    @Override
    public void onFailure(Exchange exchange) {
        if(log.isDebugEnabled()) {
            log.debug("Processing failure of Exchange id:{}", exchange.getExchangeId());
        }
        try {
            if (session != null && session.getTransacted()) {
                this.session.rollback();
            }
        } catch (JMSException e) {
            log.warn("Failed to rollback the session: {}", e.getMessage());
        }
    }
    
    /*
     * @see org.apache.camel.spi.Synchronization#onComplete(org.apache.camel.Exchange)
     *
     * @param exchange
     */
    @Override
    public void onComplete(Exchange exchange) {
        if(log.isDebugEnabled()) {
            log.debug("Processing completion of Exchange id:{}", exchange.getExchangeId());
        }
        try {
            if (session != null && session.getTransacted()) {
                this.session.commit();
            }
        } catch (JMSException e) {
            log.warn("Failed to commit the session: {}", e.getMessage());
            exchange.setException(e);
        }
    }

}
