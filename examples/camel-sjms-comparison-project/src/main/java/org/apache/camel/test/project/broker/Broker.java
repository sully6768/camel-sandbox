package org.apache.camel.test.project.broker;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Broker {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		new ClassPathXmlApplicationContext("broker.xml", Broker.class);
		System.in.read();
	}

}
