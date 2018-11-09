/*
 * Copyright (C) 2017 Institute of Communication and Computer Systems (imu.iccs.com)
 *
 * This Source Code Form is subject to the terms of the
 * Mozilla Public License, v. 2.0. If a copy of the MPL
 * was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */

package com.example.eventSubscriber.event_listener;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.example.eventSubscriber.event_listener.metricvalue.MetricValueMonitorBean;
import com.example.eventSubscriber.event_listener.properties.MetaSolverProperties;
 
@Component
@Slf4j
public class EventListenerApplicationListener implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {
 
    private MetricValueMonitorBean metricValueMonitorBean;
    private MetaSolverProperties properties;
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
		this.metricValueMonitorBean = (MetricValueMonitorBean) applicationContext.getBean(MetricValueMonitorBean.class);
		this.metricValueMonitorBean.subscribe();
		this.properties = (MetaSolverProperties) applicationContext.getBean(MetaSolverProperties.class);
    }
 
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
//		System.out.println("** Application Event Received : "+event.getClass().getName());
		log.trace("** Application Event Received : "+event.getClass().getName());
//		System.out.println(metricValueMonitorBean.getMetricValuesRegistry().getMetricValue("metricValue"));
		/*if (event instanceof org.springframework.context.event.ContextRefreshedEvent) {
			log.debug("** Application Event Received : Context Refreshed");
			metricValueMonitorBean.subscribe();
		} else*/
		if (event instanceof org.springframework.context.event.ContextClosedEvent) {
			log.debug("** Application Event Received : Context Closed");
			metricValueMonitorBean.unsubscribe();
		} else
		{
			System.out.println(""+metricValueMonitorBean.getMetricValuesRegistry().getMetricValue("metricValue"));
			System.out.println(metricValueMonitorBean.getMetricValuesRegistry().toString());
			log.trace("** Application Event Received : Other...");
		}
    }
}