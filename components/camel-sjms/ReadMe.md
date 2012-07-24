Camel SJMS Project (sjms)
====================
This project aims to create a Camel JMS component that relies strictly on the Java JMS API to 
provide robust and scalable JMS to Camel without the need for additional 3rd party APIs.

Goals
-----------
* JMS Queue & Temporary Queue Support
* JMS Topic Support
* Full Asynchronous and Synchronous Support
* Plugable Connection Resource Support
* Built in pooling and caching management of Sessions, Producers and Consumers 

The Component Service discovery ID is 'sjms'.

To Do's
-------
* Durable Subscriptions
* Batch Processing (Support List of messages)
* ITest
* Test, Test, Test
* Documentation
* More Tests


*S* stands for Simple or Standard or Springless
---------------------------------------------------------

The Simple JMS Component is a drop in replacement for the current Spring container based 
Camel-JMS component. It is currently being hosted here at github while under development.
It can be retrieved and built using the following steps:

* git clone https://github.com/sully6768/camel-sandbox
* cd camel-sandbox/components/camel-sjms
* mvn clean install

After a successful build, add the following dependency to the `pom.xml` for this component:
```xml
<dependency>
    <groupId>org.apache.camel</groupId>
    <artifactId>camel-sjms</artifactId>
    <version>x.x.x</version>
    <!-- currently supports trunk only -->
</dependency>
```


