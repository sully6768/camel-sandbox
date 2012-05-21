package org.apache.camel.component.sjms.jms;

public enum JmsMessageHeaderType {
    JMSDestination,
    JMSDeliveryMode,
    JMSExpiration,
    JMSPriority,
    JMSMessageID,
    JMSTimestamp,
    JMSCorrelationID,
    JMSReplyTo,
    JMSType,
    JMSRedelivered,
    
    /*
     * Add known custom headers
     */
    JMSXGroupID
    
}
