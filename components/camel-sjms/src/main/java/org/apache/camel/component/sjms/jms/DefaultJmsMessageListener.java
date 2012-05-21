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
package org.apache.camel.component.sjms.jms;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.transaction.TransactionManager;

import org.apache.camel.AsyncProcessor;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.component.sjms.MessageHandler;
import org.apache.camel.spi.ExceptionHandler;
import org.apache.camel.util.AsyncProcessorConverterHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sully6768
 * 
 */
public class DefaultJmsMessageListener implements MessageListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJmsMessageListener.class);
	private ExceptionHandler exceptionHandler;
	private MessageHandler messageHandler;

	/**
	 * TODO Add Constructor Javadoc
	 * 
	 * @param endpoint
	 */
	public DefaultJmsMessageListener(MessageHandler messageHandler) {
		super();
		this.messageHandler = messageHandler;
	}

	/**
	 * TODO Add override javadoc
	 * 
	 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
	 * 
	 * @param message
	 */
	@Override
	public void onMessage(Message message) {
	    getMessageHandler().handleMessage(message);
	}

	public void setExceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	public ExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

    /**
     * Sets the DefaultMessageHandler value of messageHandler for this instance of DefaultJmsMessageListener.
     *
     * @param messageHandler Sets DefaultMessageHandler, default is TODO add default
     */
    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Gets the DefaultMessageHandler value of messageHandler for this instance of DefaultJmsMessageListener.
     *
     * @return the messageHandler
     */
    public MessageHandler getMessageHandler() {
        return messageHandler;
    }
}