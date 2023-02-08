/*
 * Copyright (c) 2018-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.String.format;

import uk.ac.manchester.spinnaker.messages.scp.SCPResult;

/**
 * Indicate that a response code returned from the board was unexpected for the
 * current operation.
 */
public class UnexpectedResponseCodeException extends Exception {
	private static final long serialVersionUID = 7864690081287752744L;

	/** The response that cause this exception to be thrown, if known. */
	public final SCPResult response;

	/**
	 * Special constructor for one-way operations.
	 *
	 * @param operation
	 *            The operation being performed
	 * @param command
	 *            The command being executed
	 * @param response
	 *            The response received in error
	 * @param ignored
	 *            Ignored
	 */
	public UnexpectedResponseCodeException(String operation, Enum<?> command,
			SCPResult response, Object ignored) {
		super(format("Unexpected response %s while performing one-way "
				+ "operation %s using command %s", response, operation,
				command));
		this.response = response;
	}

	/**
	 * @param operation
	 *            The operation being performed
	 * @param command
	 *            The command being executed
	 * @param response
	 *            The response received in error
	 */
	public UnexpectedResponseCodeException(String operation, Enum<?> command,
			SCPResult response) {
		super(format("Unexpected response %s while performing operation %s "
				+ "using command %s", response.name(), operation,
				command.name()));
		this.response = response;
	}
}
