/*
 * Copyright (C) 2017 Institute of Communication and Computer Systems (imu.iccs.com)
 *
 * This Source Code Form is subject to the terms of the
 * Mozilla Public License, v. 2.0. If a copy of the MPL
 * was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */

package com.example.eventSubscriber.event_listener.metricvalue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MetricValueRegistry<T> {
	private HashMap<String,MetricValue<T>> registry = new HashMap<>();
	
	public T getMetricValue(String metricName) {
		MetricValue<T> mv = registry.get(metricName);
		if (mv==null) return null;
		return mv.getMetricValue();
	}
	
	public void setMetricValue(String metricName, T metricValue) {
		setMetricValue(metricName, metricValue, System.currentTimeMillis());
	}
	
	public void setMetricValue(String metricName, T metricValue, long timestamp) {
		registry.put(metricName, new MetricValue(metricValue, timestamp));
	}
	
	public Map<String,String> getMetricValuesAsMap() {
		HashMap<String,String> mvm = new HashMap<>();
		for (Map.Entry<String,MetricValue<T>> entry : registry.entrySet()) {
			String name = entry.getKey();
			T mv = entry.getValue().getMetricValue();
			String value = mv!=null ? mv.toString(): "";
			mvm.put(name, value);
		}
		return mvm;
	}
	
	// MetricValue class definition
	private static class MetricValue<T> {
		private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		
		private T value;
		private long timestamp;
		
		public MetricValue(T value, long timestamp) {
			this.value = value;
			this.timestamp = timestamp;
		}
		
		public T getMetricValue() { return value; }
		public long getTimestamp() { return timestamp; }
		
		public String toString() {
			return value+"_@_"+sdf.format(new Date(timestamp));
		}
	}
	
	public String toString() {
		return super.toString()+" : "+registry.toString();
	}
}
