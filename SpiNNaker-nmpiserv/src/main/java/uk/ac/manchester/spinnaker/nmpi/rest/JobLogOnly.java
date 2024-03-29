/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.rest;

/**
 * A Job where only the log is set (to allow log updates).
 */
public class JobLogOnly {
	private String log;

	/**
	 * Create a job with only a log.
	 *
	 * @param log
	 *            The log to set.
	 */
	public JobLogOnly(String log) {
		this.log = log;
	}

	/**
	 * Get the log.
	 *
	 * @return The log
	 */
	public String getLog() {
		return log;
	}

	/**
	 * Set the log.
	 *
	 * @param log
	 *            The log to set
	 */
	public void setLog(String log) {
		this.log = log;
	}
}
