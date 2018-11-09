/*
 * Copyright (C) 2017 Institute of Communication and Computer Systems (imu.iccs.com)
 *
 * This Source Code Form is subject to the terms of the
 * Mozilla Public License, v. 2.0. If a copy of the MPL
 * was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */

package com.example.eventSubscriber.event_listener.metricvalue;



import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;
import javax.jms.*;
import javax.jms.JMSException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MetricValueListener implements MessageListener {
	
	private MessageConsumer consumer;
	private Topic topic;
	private String topicName;
	private TopicType type;
	private MetricValueRegistry<Object> registry;
	private Gson gson;
//	private Coordinator coordinator;
	
	public MetricValueListener(MessageConsumer consumer, Topic topic, TopicType type, MetricValueRegistry<Object> registry) throws JMSException {
		log.debug("MetricValueListener.<init>: type={}", type);
//		this.coordinator = coordinator;
		this.consumer = consumer;
		this.topic = topic;
		this.topicName = topic.getTopicName();
		this.type = type;
		this.registry = registry;
		gson = new Gson();
	}
	
	public void onMessage(Message message) { 
		try {
			log.debug("Listener of topic {}: Received message: ", topic.getTopicName());
			if (message instanceof TextMessage) { 
				// Extract Topic name and payload from message
				TextMessage textMessage = (TextMessage) message; 
				//String metricName = textMessage.getStringProperty("topic_name");
				String metricName = topicName;
				String payload = textMessage.getText();
				System.out.println("Metric: {}"+ metricName);
				System.out.println("Type:   {}"+type);
				System.out.println("Payload:\n{}"+ payload);
				
				switch (type) {
					case BYTES_READ:
						log.debug("Listener of topic {}: Got an MVV event: ", topicName);
						processMetricValueEvent(metricName, payload);
						break;
					case BYTES_WRITTEN:
						log.debug("Listener of topic {}: Got a SCALE event: ", topicName);
						processMetricValueEvent(metricName, payload);
						break;
					default:
						log.debug("Listener of topic {}: Got a UNKNOWN event: Ignoring it", topicName);
				}
			} else {
				log.warn("Unsupported message type: {}", message.getClass().getName());
			}
		} catch (JMSException e) { 
			log.error("Caught: {}", e); 
			//e.printStackTrace(); 
		} 
	}

	protected void processMetricValueEvent(String metricName, String payload) {
		if (metricName!=null && ! (metricName=metricName.trim()).isEmpty()) {
			// Extract key-value pairs from message payload
			// ...using MetricValueEvent
			log.debug("Listener of topic {}: Converting event payload to MetricValueEvent instance...", topicName);
			MetricValueEvent event = gson.fromJson(payload, MetricValueEvent.class);
			log.debug("Listener of topic {}: MetricValueEvent instance: {}", topicName, event);
			// ...using Map
			/*Type type = new TypeToken<Map<String,Object>>(){}.getType();
			Map<String,Object> metricValueMap = gson.fromJson(payload, type);
			metricValueMap.forEach((x,y)-> System.out.println("\t" + x + " : " + y + " (" + y.getClass().getName() +")"));*/
			
			// Cache Metric Value in registry
			log.debug("Listener of topic {}: Metric registry values BEFORE update: {}", topicName, registry);
			registry.setMetricValue( metricName, event.getMetricValue()/*event.getMetric_value()*/ );
			log.info("Metric Value set: name='{}', value='{}', topic={}", metricName, event.getMetricValue(),/*event.getMetric_value(),*/ topicName);
			log.debug("Listener of topic {}: Metric registry values AFTER update:  {}", topicName, registry);
		} else {
			log.warn("Missing property: 'topic_name'");
		}
	}
	
	protected void processScaleEvent(String metricName, String payload) {
		log.debug("Listener of topic {}: Calling coordinator to start Scaling process...", topicName);
//		coordinator.requestStartProcessForScaling();
	}
}
