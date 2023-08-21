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

	/** An output string to report. */
	@JsonProperty("plugin_output")
	private String pluginOutput;

	/** Optional performance data to report. */
	@JsonProperty("performance_data")
	private String performanceData;

	/** Optional durations in seconds of the test result. */
	@JsonProperty("ttl")
	private Integer ttl;

	/** The target host being reported on. */
	@JsonProperty("host")
	private String host;

	/** Optional target service being reported on. */
	@JsonProperty("service")
	private String service;

	/**
	 * Create a new result to report.
	 *
	 * @param exitStatus
	 *            The status to report.
	 * @param pluginOutput
	 *            An output string to add to the report.
	 * @param performanceData
	 *            Any performance data to report as a string.
	 * @param ttl
	 *            The time at which the next report is expected in seconds.
	 * @param host
	 *            The host to report on.
	 * @param service
	 *            The service to report on.
	 */
	public Icinga2CheckResult(int exitStatus, String pluginOutput,
			String performanceData, Integer ttl, String host, String service) {
		this.exitStatus = exitStatus;
		this.pluginOutput = pluginOutput;
		this.performanceData = performanceData;
		this.ttl = ttl;
		this.host = host;
		this.service = service;
	}
}
