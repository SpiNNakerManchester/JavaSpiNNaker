/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.scp;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP response that should never be received because its request is
 * guaranteed to be a one-way request.
 */
public class NoResponse extends SCPResponse {
	/**
	 * Create an instance.
	 *
	 * @param operation
	 *            The overall operation that we are doing.
	 * @param buffer
	 *            The buffer holding the response data.
	 * @throws UnexpectedResponseCodeException
	 *             Always.
	 */
	public NoResponse(String operation, ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		super(buffer);
		throw new UnexpectedResponseCodeException(operation, "one-way request",
				result.toString());
	}
}
