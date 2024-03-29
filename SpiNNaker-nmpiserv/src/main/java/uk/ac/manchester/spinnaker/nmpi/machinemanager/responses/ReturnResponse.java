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
 * A response that is the successful result of a request.
 */
public class ReturnResponse implements Response {
	/** The value returned. */
	private String returnValue;

	/**
	 * Get the value returned.
	 *
	 * @return The value
	 */
	public String getReturnValue() {
		return returnValue;
	}

	/**
	 * Set the value returned.
	 *
	 * @param returnValue
	 *            The value to set
	 */
	@JsonSetter("return")
	public void setReturnValue(final JsonNode returnValue) {
		this.returnValue = returnValue.toString();
	}
}
