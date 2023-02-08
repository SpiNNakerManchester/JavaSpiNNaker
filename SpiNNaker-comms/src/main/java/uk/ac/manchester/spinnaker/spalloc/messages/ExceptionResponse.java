/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * A response to a request that indicates a failure.
 */
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
public final class ExceptionResponse implements Response {
	private String exception;

	/** @return The exception message. Should not include a stack trace. */
	public String getException() {
		return exception;
	}

	void setException(String exception) {
		this.exception = exception == null ? "" : exception.toString();
	}
}
