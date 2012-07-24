package org.apache.camel.test.project.si;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.message.GenericMessage;
import org.springframework.util.StopWatch;

public class SiJmsRequestReply {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		new ClassPathXmlApplicationContext("consumer.xml", SiJmsRequestReply.class);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("producer.xml", SiJmsRequestReply.class);
		RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
		for (int i = 0; i < 1000000; i++) {
			testBatchOfMessages(gateway, 1000);
		}
	}

	private static void testBatchOfMessages(RequestReplyExchanger gateway, int number){
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for (int i = 0; i < number; i++) {
			gateway.exchange(new GenericMessage<Integer>(i));
		}
		stopWatch.stop();
		System.out.println("Exchanged " + number + "  messages in " + stopWatch.getTotalTimeMillis());
	}

}
