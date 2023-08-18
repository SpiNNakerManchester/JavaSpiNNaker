/*
 * Copyright (c) 2020 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.nmpi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A result to report to Icinga.
 *
 */
@JsonInclude(Include.NON_NULL)
public class Icinga2CheckResult {

	/**
	 * The exit status of the service or host.
	 * For services, 0=OK, 1=WARNING, 2=CRITICAL, 3=UNKNOWN.
	 * For hosts, 0=OK, 1=CRITICAL
	 */
	@JsonProperty("exit_status")
	private int exitStatus;

	/**
	 * An output string to report.
	 */
	@JsonProperty("plugin_output")
	private String pluginOutput;

	/**
	 * Optional performance data to report.
	 */
	@JsonProperty("performance_data")
	private String performanceData;

	/**
	 * Optional durations in seconds of the test result.
	 */
	@JsonProperty("ttl")
	private Integer ttl;

	/**
	 * The target host being reported on.
	 */
	@JsonProperty("host")
	private String host;

	/**
	 * Optional target service being reported on.
	 */
	@JsonProperty("service")
	private String service;

	/**
	 * Create a new result to report.
	 *
	 * @param exitStatusParam The status to report.
	 * @param pluginOutputParam An output string to add to the report.
	 * @param performanceDataParam Any performance data to report as a string.
	 * @param ttlParam The time at which the next report is expected in seconds.
	 * @param hostParam The host to report on.
	 * @param serviceParam The service to report on.
	 */
	public Icinga2CheckResult(final int exitStatusParam,
			final String pluginOutputParam, final String performanceDataParam,
			final Integer ttlParam, final String hostParam,
			final String serviceParam) {
		exitStatus = exitStatusParam;
		pluginOutput = pluginOutputParam;
		performanceData = performanceDataParam;
		ttl = ttlParam;
		host = hostParam;
		service = serviceParam;
	}
}
