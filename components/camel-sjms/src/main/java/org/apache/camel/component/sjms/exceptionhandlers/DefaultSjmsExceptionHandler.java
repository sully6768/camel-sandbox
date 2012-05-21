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
package org.apache.camel.component.sjms.exceptionhandlers;

import javax.jms.JMSException;

import org.apache.camel.Exchange;
import org.apache.camel.component.sjms.SjmsExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class DefaultSjmsExceptionHandler implements SjmsExceptionHandler {
    
    protected transient Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void onException(JMSException exception) {
		if (exception != null)
			handleException(exception);
	}

    @Override
    public void handleException(Throwable exception) {
        this.handleException(exception.getLocalizedMessage(), exception);
    }

    @Override
    public void handleException(String message, Throwable exception) {
        if(logger.isDebugEnabled()) {
            logger.debug("Exception Thrown: " + message, exception);
        }
        
    }

    @Override
    public void handleException(String message, Exchange exchange,
            Throwable exception) {
        if(logger.isDebugEnabled()) {
            logger.debug("Exception Thrown: " + message + ".  Exhange ID: " + exchange.getExchangeId(), exception);
        }
    }
}
