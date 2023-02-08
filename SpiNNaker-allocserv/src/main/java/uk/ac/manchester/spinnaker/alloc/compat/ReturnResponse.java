/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.compat;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A message indicating an operation succeeded.
 *
 * @author Donal Fellows
 */
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
final class ReturnResponse {
	private Object returnValue;

	ReturnResponse(Object value) {
		returnValue = value;
	}

	@JsonProperty("return")
	public Object getReturnValue() {
		return returnValue;
	}

	void setReturnValue(Object returnValue) {
		this.returnValue = returnValue;
	}
}
