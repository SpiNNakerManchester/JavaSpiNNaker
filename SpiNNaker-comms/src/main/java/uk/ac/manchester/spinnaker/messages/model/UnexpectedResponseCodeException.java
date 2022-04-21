/*
 * Copyright (c) 2018-2022 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

	/** The response received in error. */
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
