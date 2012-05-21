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
package org.apache.camel.component.sjms.messagehandlers;

import static org.apache.camel.component.sjms.SjmsConstants.JMS_MESSAGE_TYPE;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.sjms.JmsMessageHelper;
import org.apache.camel.component.sjms.JmsMessageType;
import org.apache.camel.component.sjms.MessageHandler;
import org.apache.camel.component.sjms.jms.SessionAcknowledgementType;
import org.apache.camel.spi.ExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Add Class documentation for DefaultMessageHandler
 *
 * @author sully6768
 */
public class DefaultMessageHandler implements MessageHandler {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private Endpoint endpoint;
    private ExceptionHandler exceptionHandler;
    private AsyncProcessor processor;
    private Session session;
    private boolean transacted = false;
    private SessionAcknowledgementType acknowledgementType = SessionAcknowledgementType.AUTO_ACKNOWLEDGE;
    private boolean async = false;
    
    /**
     * TODO Add Constructor Javadoc
     *
     * @param endpoint
     * @param processor
     */
    public DefaultMessageHandler() {
        super();
    }

    /**
     * @param message
     */
    public void handleMessage(Message message) {
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("Start the message handler");
            LOGGER.trace("Message received: ");
        }
        final Exchange exchange = createExchange();

        try {
            processMessage(message, exchange);
        } catch (Exception e) {
            exchange.setException(e);
        } finally {
            // log exception if an exception occurred and was not handled
            if (exchange.getException() != null) {
                exceptionHandler.handleException(
                        "Error processing exchange", exchange,
                        exchange.getException());
            }
        }
    }
    
    protected Exchange createExchange() {
        return endpoint.createExchange();
    }

    /**
     * @param message
     * @param exchange
     * @throws JMSException
     */
    protected Exchange processMessage(Message message, final Exchange exchange)
            throws Exception {
        JmsMessageHelper.setJmsMessageHeaders(message, exchange);
        if(message != null) {
            // convert to JMS Message of the given type
            switch (JmsMessageHelper.discoverType(message)) {
            case Bytes:
                processBytesMessage(message, exchange);
                break;
            case Map:
                processMapMessage(message, exchange);
                break;
            case Object:
                processObjectMessage(message, exchange);
                break;
            case Message:
                // Do nothing.  Only set the headers for an empty message
                break;
            case Text:
                processTextMessage(message, exchange);
                break;
            default:
                break;
            }
        }
        if(isAsync() || isTransacted()) {
            processor.process(exchange, new MessageHanderAsyncCallback(exchange));
        } else {
            processor.process(exchange);
        }
        return exchange;
    }

    /**
     * @param message
     * @param exchange
     * @throws JMSException
     */
    protected Exchange processTextMessage(Message message, final Exchange exchange)
            throws JMSException {
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("Decoding the Text Message");
        }
        
        TextMessage textMsg = (TextMessage) message;
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("Text Message:  " + textMsg);
        }
        
        exchange.getIn().setHeader(JMS_MESSAGE_TYPE, JmsMessageType.Text);
        exchange.getIn().setBody(textMsg.getText());
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("Finished decoding the Text Message");
        }
        return exchange;
    }

    /**
     * @param message
     * @param exchange
     * @throws JMSException
     */
    protected Exchange processObjectMessage(Message message, final Exchange exchange)
            throws JMSException {
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("Decoding the Object Message");
        }
        
        ObjectMessage objMsg = (ObjectMessage) message;
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("Object Message:  " + objMsg);
        }
        
        exchange.getIn().setHeader(JMS_MESSAGE_TYPE, JmsMessageType.Object);
        exchange.getIn().setBody(objMsg.getObject());
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("Finished decoding the Object Message");
        }
        return exchange;
    }

    /**
     * @param message
     * @param exchange
     * @throws JMSException
     */
    @SuppressWarnings("unchecked")
    protected Exchange processMapMessage(Message message, final Exchange exchange)
            throws JMSException {
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("Decoding Map Message");
        }
        HashMap<String, Object> body = new HashMap<String, Object>();
        MapMessage mapMessage = (MapMessage) message;
        Enumeration<String> names = mapMessage.getMapNames();
        while(names.hasMoreElements()) {
            String key = names.nextElement();
            Object value = mapMessage.getObject(key);
            body.put(key, value);
        }

        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("Map Message:  " + body);
        }
        
        exchange.getIn().setHeader(JMS_MESSAGE_TYPE, JmsMessageType.Map);
        exchange.getIn().setBody(body);
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("Finished decoding Map Message");
        }
        return exchange;
    }

    /**
     * @param message
     * @param exchange
     * @throws JMSException
     */
    protected Exchange processBytesMessage(Message message, final Exchange exchange)
            throws JMSException {
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("Decoding the Byte Message");
        }
        BytesMessage bytesMessage = (BytesMessage) message;

        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("  Received containing " + bytesMessage.getBodyLength() + " bytes");
        }
        ArrayList<Byte> bytesList = new ArrayList<Byte>();
        for(long i = 0; i < bytesMessage.getBodyLength(); ++i) {
            bytesList.add(bytesMessage.readByte());
        }
        bytesList.trimToSize();
        Byte[] bytes = new Byte[bytesList.size()];
        bytes = bytesList.toArray(bytes);        
        exchange.getIn().setHeader(JMS_MESSAGE_TYPE, JmsMessageType.Bytes);
        exchange.getIn().setBody(bytes);
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("Finished decoding the Byte Message");
        }
        return exchange;
    }
    

    
    /**
     * Sets the boolean value of transacted for this instance of DefaultMessageHandler.
     *
     * @param transacted Sets boolean, default is TODO add default
     */
    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }

    /**
     * Gets the boolean value of transacted for this instance of DefaultMessageHandler.
     *
     * @return the transacted
     */
    public boolean isTransacted() {
        return transacted;
    }
    
    protected class MessageHanderAsyncCallback implements AsyncCallback {
        
        private Exchange exchange;
        

        /**
         * TODO Add Constructor Javadoc
         *
         * @param xid
         * @param exchange
         */
        private MessageHanderAsyncCallback(Exchange exchange) {
            super();
            this.exchange = exchange;
        }             

        @Override
        public void done(boolean sync) {
            if (exchange.isFailed()) {
                if(isTransacted()) {
                    if(getSession() != null) {
                        try {
                            getSession().rollback();
                        } catch (JMSException e) {
                            throw new RuntimeCamelException(
                                    "Unable to rollback the transaction. " +
                                    "Error: " + e.getErrorCode() + " - " + e.getLocalizedMessage(), e);
                        }
                    }
                } else {
                    if(exceptionHandler != null) {
                        exceptionHandler.handleException(exchange.getException());
                    } else {
                        LOGGER.warn("Exchange failed.  Dropping message.");
                    }
                }
            } else {
                if(isTransacted()) {
                    if(getSession() != null) {
                        try {
                            getSession().commit();
                        } catch (JMSException e) {
                            throw new RuntimeCamelException(
                                    "Unable to commit the transaction. " +
                                    "Error: " + e.getErrorCode() + " - " + e.getLocalizedMessage(), e);
                        }
                    }
                } else {
                    if(exceptionHandler != null) {
                        exceptionHandler.handleException(exchange.getException());
                    } else {
                        LOGGER.warn("Exchange failed.  Dropping message.");
                    }
                }
            }
        }
        
    }

    /**
     * Gets the SimpleJmsEndpoint value of endpoint for this instance of DefaultMessageHandler.
     *
     * @return the endpoint
     */
    public Endpoint getEndpoint() {
        return endpoint;
    }

    /**
     * Sets the SimpleJmsEndpoint value of endpoint for this instance of DefaultMessageHandler.
     *
     * @param endpoint Sets SimpleJmsEndpoint, default is TODO add default
     */
    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Gets the SjmsExceptionHandler value of exceptionHandler for this instance of DefaultMessageHandler.
     *
     * @return the exceptionHandler
     */
    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    /**
     * Sets the SjmsExceptionHandler value of exceptionHandler for this instance of DefaultMessageHandler.
     *
     * @param exceptionHandler Sets SjmsExceptionHandler, default is TODO add default
     */
    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Gets the AsyncProcessor value of processor for this instance of DefaultMessageHandler.
     *
     * @return the processor
     */
    public AsyncProcessor getProcessor() {
        return processor;
    }

    /**
     * Sets the AsyncProcessor value of processor for this instance of DefaultMessageHandler.
     *
     * @param processor Sets AsyncProcessor, default is TODO add default
     */
    public void setProcessor(AsyncProcessor processor) {
        this.processor = processor;
    }

    /**
     * Gets the SessionAcknowledgementType value of acknowledgementType for this instance of DefaultMessageHandler.
     *
     * @return the acknowledgementType
     */
    public SessionAcknowledgementType getAcknowledgementType() {
        return acknowledgementType;
    }

    /**
     * Sets the SessionAcknowledgementType value of acknowledgementType for this instance of DefaultMessageHandler.
     *
     * @param acknowledgementType Sets SessionAcknowledgementType, default is TODO add default
     */
    public void setAcknowledgementType(
            SessionAcknowledgementType acknowledgementType) {
        this.acknowledgementType = acknowledgementType;
    }

    /**
     * Sets the Session value of session for this instance of DefaultMessageHandler.
     *
     * @param session Sets Session, default is TODO add default
     */
    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * Gets the Session value of session for this instance of DefaultMessageHandler.
     *
     * @return the session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Sets the boolean value of async for this instance of DefaultMessageHandler.
     *
     * @param async Sets boolean, default is TODO add default
     */
    public void setAsync(boolean async) {
        this.async = async;
    }

    /**
     * Gets the boolean value of async for this instance of DefaultMessageHandler.
     *
     * @return the async
     */
    public boolean isAsync() {
        return async;
    }
}
