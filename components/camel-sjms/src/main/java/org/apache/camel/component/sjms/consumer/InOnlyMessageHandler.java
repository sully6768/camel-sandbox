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
package org.apache.camel.component.sjms.consumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.spi.Synchronization;
import org.apache.camel.util.AsyncProcessorHelper;

/**
 * TODO Add Class documentation for DefaultMessageHandler
 * 
 * @author sully6768
 */
public class InOnlyMessageHandler extends DefaultMessageHandler {

    /**
     * TODO Add Constructor Javadoc
     * 
     * @param endpoint
     * @param processor
     */
    public InOnlyMessageHandler(Endpoint endpoint, AtomicBoolean stopped,
            ExecutorService executor) {
        this(endpoint, stopped, executor, null);
    }

    /**
     * TODO Add Constructor Javadoc
     * 
     * @param stopped
     * @param synchronization
     */
    public InOnlyMessageHandler(Endpoint endpoint, AtomicBoolean stopped,
            ExecutorService executor, Synchronization synchronization) {
        super(endpoint, stopped, executor, synchronization);
    }

    /**
     * @param message
     */
    @Override
    public void doHandleMessage(final Exchange exchange) {
        if (log.isDebugEnabled()) {
            log.debug("SjmsMessageConsumer invoked for Exchange id:{} ",
                    exchange.getExchangeId());
        }
        if (isStarted()) {
            if (exchange.isFailed()) {
                return;
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Processing Exchange id:{} synchronously",
                            exchange.getExchangeId());
                }
                try {
                    AsyncProcessorHelper.process(getProcessor(), exchange);
                } catch (Exception e) {
                    exchange.setException(e);
                }
            }
        } else {
            log.warn(
                    "SjmsMessageConsumer invoked while stopped.  Exchange id:{} will not be processed.",
                    exchange.getExchangeId());
        }

        if (log.isDebugEnabled()) {
            log.debug("SjmsMessageConsumer invoked for Exchange id:{} ",
                    exchange.getExchangeId());
        }
    }
    
    @Override
    public void close() {
    }
}
