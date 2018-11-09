/*
 * Copyright (C) 2017 Institute of Communication and Computer Systems (imu.iccs.com)
 *
 * This Source Code Form is subject to the terms of the
 * Mozilla Public License, v. 2.0. If a copy of the MPL
 * was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */

package com.example.eventSubscriber.event_listener.metricvalue;

import java.util.List;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(doNotUseGetters = true, exclude={"host_name", /*"component_name",*/ "level"})
public class MetricValueEvent {
	private String metricValue;
	//private String cloudName;
	//private List<String> componentName;
	private int level;
	private long timestamp;
}
