/*
 * Copyright (C) 2017 Institute of Communication and Computer Systems (imu.iccs.com)
 *
 * This Source Code Form is subject to the terms of the
 * Mozilla Public License, v. 2.0. If a copy of the MPL
 * was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */

package com.example.eventSubscriber.event_listener.metricvalue;


import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jms.*;
import javax.jms.JMSException;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.apache.activemq.ActiveMQConnection; 
import org.apache.activemq.ActiveMQConnectionFactory; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import com.example.eventSubscriber.event_listener.properties.MetaSolverProperties;

@Service
@Slf4j
public class MetricValueMonitorBean implements ApplicationContextAware {
	
	private MetaSolverProperties properties;

    
	private HashMap<String,ConnectionConf> connectionCache = new HashMap<>();
	private MetricValueRegistry<Object> registry = new MetricValueRegistry<>();
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		//this.applicationContext = applicationContext;
		this.properties = (MetaSolverProperties) applicationContext.getBean(MetaSolverProperties.class);
		log.debug("MetaSolver.MetricValueMonitorBean: setApplicationContext(): configuration={}", properties);
	}
	
	public MetricValueRegistry<Object> getMetricValuesRegistry() {
		return registry;
	}
	
	public void subscribe() {
		// Check if Pub/Sub should be activated
		if (properties.getPubsub().isOn()==false) {
			log.info("*****   Pub/Sub is SWITCHED OFF");
			return;
		}
		System.out.println("Subscribing to topics:");
		// Subscribe to configured topics
		log.debug("Subscribing to topics: ");
		for (MetaSolverProperties.Pubsub.Topic pst : properties.getPubsub().getTopics()) {
			// Get topic configuration
			String url = pst.getUrl();
			String topicName = pst.getName();
			String clientId = pst.getClientId();
			TopicType type = pst.getType();
			if (url==null || url.trim().isEmpty() || url.trim().equalsIgnoreCase("DEFAULT_BROKER_URL")) url = ActiveMQConnection.DEFAULT_BROKER_URL; else url = url.trim();
			if (topicName==null || topicName.trim().isEmpty()) topicName.toString();	// Cause exception to be thrown if topicName is missing
			if (clientId==null || clientId.trim().isEmpty()) clientId = ""; else clientId = clientId.trim(); 
			if (type==null) type = TopicType.UNKNOWN;
			log.debug("Topic : {}", pst);
			
			// Subscribe to topic
			_do_subscribe(url, topicName, clientId, type);
		}
		log.debug("Subscribing to topics: ok");
	}
	
	public void subscribe(String url, String topicName, String clientId, TopicType type) {
		// Check if Pub/Sub should be activated
		if (properties.getPubsub().isOn()==false) {
			log.info("*****   Pub/Sub is SWITCHED OFF");
			return;
		}
		
		_do_subscribe(url, topicName, clientId, type);
	}
	
	protected void _do_subscribe(String url, String topicName, String clientId, TopicType type) {
		try {
			System.out.println("*****   SUBSCRIBE:\n  URL      :" + url +" {}\n  Topic    : " + topicName +   "Client-Id: {}"+ clientId+"  Type     : "+type);
			log.debug("*****   SUBSCRIBE:\n  URL      : {}\n  Topic    : {}\n  Client-Id: {}\n  Type     : {}", url, topicName, clientId, type);
			
			// Get ActiveMQ connection to the server 
			log.trace("*****   SUBSCRIBE: connection factory created: url={}", url);
			ConnectionConf cconf = connectionCache.get(url);
			if (cconf==null) {
				ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
				Connection connection = connectionFactory.createConnection();
				log.trace("*****   SUBSCRIBE: connection created");
				if ( ! clientId.isEmpty()) {
					connection.setClientID(clientId);
					log.trace("*****   SUBSCRIBE: client id set: {}", clientId);
				}
				
				cconf = new ConnectionConf();
				cconf.setUrl( url );
				cconf.setConnectionFactory( connectionFactory );
				cconf.setConnection( connection );
				
				connectionCache.put(url, cconf);
				
				// Open JMS session  (We use sessions not transactions)
				Session session = cconf.getConnection().createSession(false, Session.AUTO_ACKNOWLEDGE); 
				log.trace("*****   SUBSCRIBE: session created");
				
				SessionConf sconf = new SessionConf();
				sconf.setSession( session );
				cconf.getSessions().add( sconf );
			}
			
			// Currently, only one session per connection is supported
			SessionConf sconf = cconf.getSessions().get(0);
			Session session = sconf.getSession();
			
			// Subscribe to topic  (We use sessions not transactions)
			Topic topic = session.createTopic( topicName ); 
			log.trace("*****   SUBSCRIBE: topic created: {}", topicName);
			MessageConsumer consumer = session.createConsumer(topic);
			log.trace("*****   SUBSCRIBE: consumer created: topic={}", topicName);
			
			TopicConf tconf = new TopicConf();
			tconf.setTopic( topic );
			tconf.setConsumer( consumer );
			
			sconf.getTopics().add( tconf );
			
			// Add message listener to receive incoming messages
			MessageListener lsnr = getListener(consumer, topic, type);
			consumer.setMessageListener(lsnr);
			log.trace("*****   SUBSCRIBE: listener added");
			
			// Ready to go
			cconf.getConnection().start();
			log.trace("*****   SUBSCRIBE: connection started");
			
			log.info("*****   SUBSCRIBE: ok");
			
		} catch (Exception e) {
			log.error("*****   SUBSCRIBE: ERROR: {}", e);
			//e.printStackTrace(System.err);
		}
	}
	
	private MessageListener getListener(MessageConsumer consumer, Topic topic, TopicType type) throws JMSException {
//		MessageListener listener = new MetricValueListener(coordinator, consumer, topic, type, registry);
		MessageListener listener = new MetricValueListener( consumer, topic, type, registry);
		return listener;
	}
	
	public void unsubscribe() {
		try {
			// Check if Pub/Sub should be activated
			if (properties.getPubsub().isOn()==false) {
				log.debug("*****   Pub/Sub is SWITCHED OFF");
				return;
			}
			
			log.debug("*****   UN-SUBSCRIBE:");
			
			// Stop and close consumer, JMS session and ActiveMQ connection
			for (ConnectionConf cconf : connectionCache.values()) {
				try {
					log.debug("  Closing connection to url: {}", cconf.getUrl());
					
					for (SessionConf sconf : cconf.getSessions()) {
						try {
							log.debug("    Closing session: {}", sconf);
							
							for (TopicConf tconf : sconf.getTopics()) {
								String topicName = tconf.getTopic()!=null ? tconf.getTopic().getTopicName() : null;
								try {
									log.debug("      Unsubscribing from topic: {}", topicName);
									tconf.getConsumer().close();
									log.debug("      Unsubscribing from topic: {} : ok", topicName);
								} catch (Exception e) {
									log.error("      Unsubscribing from topic: {} : ERROR: {}", topicName, e);
									//e.printStackTrace(System.err);
								}
							}
							
							sconf.getSession().close();
							log.debug("    Closing session: {} : ok", sconf);
						} catch (Exception e) {
							log.error("    Closing session: {} : ERROR: {}", sconf, e);
							//e.printStackTrace(System.err);
						}
					}
					
					cconf.getConnection().stop();
					log.debug("  Closing connection to url: {} : ok", cconf.getUrl());
				} catch (Exception e) {
					log.error("  Closing connection to url: {} : ERROR: {}", cconf.getUrl(), e);
					//e.printStackTrace(System.err);
				}
			}
				
			log.info("*****   UN-SUBSCRIBE: ok");
			
		} catch (Exception e) {
			log.error("*****   SUBSCRIBE: ERROR: {}", e);
			//e.printStackTrace(System.err);
		}
	}
	
	
	@Getter
	@Setter
	private class ConnectionConf {
		private String url;
		private ConnectionFactory connectionFactory;
		private Connection connection;
		private List<SessionConf> sessions = new ArrayList<>();
	}
	
	@Getter
	@Setter
	private class SessionConf {
		private Session session;
		private List<TopicConf> topics = new ArrayList<>();
	}
	
	@Getter
	@Setter
	private class TopicConf {
		private Topic topic;
		private MessageConsumer consumer;
	}
}
