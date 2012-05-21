/*
 * Copyright 2012 FuseSource
 *
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
package org.apache.camel.component.sjms.jms.queue;

import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;

/**
 * TODO Add Class documentation for QueueConsumer
 *
 */
public abstract class QueueConsumer extends DefaultConsumer {

    private boolean async = false;
    private boolean transacted = false;

    /**
     * TODO Add Constructor Javadoc
     *
     * @param endpoint
     * @param processor
     */
    public QueueConsumer(Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    /**
     * Sets the boolean value of async for this instance of QueueListenerConsumer.
     *
     * @param async Sets boolean, default is TODO add default
     */
    public void setAsync(boolean async) {
        this.async = async;
    }

    /**
     * Gets the boolean value of async for this instance of QueueListenerConsumer.
     *
     * @return the async
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * Sets the boolean value of transacted for this instance of QueueConsumer.
     *
     * @param transacted Sets boolean, default is TODO add default
     */
    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }

    /**
     * Gets the boolean value of transacted for this instance of QueueConsumer.
     *
     * @return the transacted
     */
    public boolean isTransacted() {
        return transacted;
    }
    
}
