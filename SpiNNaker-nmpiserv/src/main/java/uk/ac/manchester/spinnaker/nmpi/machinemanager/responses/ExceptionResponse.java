/*
 * Copyright (c) 2014 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.machinemanager.responses;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A response to a request that indicates a failure.
 */
public class ExceptionResponse implements Response {
	/** The exception to report. */
	private String exception;

	/**
	 * Get the exception to report.
	 *
	 * @return The exception
	 */
	public String getException() {
		return exception;
	}

	/**
	 * Set the exception to report.
	 *
	 * @param exception
	 *            The exception to set
	 */
	@JsonSetter("exception")
	public void setException(final JsonNode exception) {
		this.exception = exception.toString();
	}
}
