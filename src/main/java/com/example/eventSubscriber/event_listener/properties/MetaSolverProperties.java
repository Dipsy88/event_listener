/*
 * Copyright (C) 2017 Institute of Communication and Computer Systems (imu.iccs.com)
 *
 * This Source Code Form is subject to the terms of the
 * Mozilla Public License, v. 2.0. If a copy of the MPL
 * was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */

package com.example.eventSubscriber.event_listener.properties;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import com.example.eventSubscriber.event_listener.metricvalue.TopicType;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Validated
@Configuration
@ConfigurationProperties
@Slf4j
@PropertySource("file:${MELODIC_CONFIG_DIR}/eu.melodic.upperware.dlms.properties")
public class MetaSolverProperties {

	@Valid
	@NotNull
	private Esb esb;
	
	@Getter
	@Setter
	public static class Esb {
		@NotBlank
		private String url;
	}
	
	// --------------------------------------------------------------
	
	@Valid
	@NotNull
	private Pubsub pubsub;

	@Getter
	@Setter
	@ToString
	public static class Pubsub {
		private String on;

		private List<Topic> topics;

		public boolean isOn() {
			return booleanValue(on);
		}

		@Getter
		@Setter
		@ToString
		public static class Topic {
			@NotBlank
			private String name;
			@NotBlank
			private String url;
			private String clientId;
			private TopicType type;
		}
	}

	private static boolean booleanValue(String str) {
		return booleanValue(str, false);
	}

	private static boolean booleanValue(String str, boolean defVal) {
		if (StringUtils.isBlank(str)) return defVal;
		return BooleanUtils.toBooleanObject(str);
	}
	
	// --------------------------------------------------------------

	@Valid
	@NotNull
	private double utilityThresholdFactor;
}
