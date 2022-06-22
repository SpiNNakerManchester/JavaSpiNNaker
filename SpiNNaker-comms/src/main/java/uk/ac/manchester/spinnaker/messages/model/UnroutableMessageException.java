/*
 * Copyright (c) 2022 The University of Manchester
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

import uk.ac.manchester.spinnaker.messages.scp.SCPResult;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/**
 * Indicate that the board (or BMP) was unable to send a message on to its
 * final destination.
 */
public class UnroutableMessageException
		extends UnexpectedResponseCodeException {
	private static final long serialVersionUID = 2106128799950032057L;

	/** The full header from the response message. */
	public final SDPHeader header;

	/**
	 * @param operation
	 *            The overall operation that we were doing.
	 * @param command
	 *            The command that we were handling a response to.
	 * @param header
	 *            The header that indicated a problem with routing.
	 */
	public UnroutableMessageException(String operation, Enum<?> command,
			SDPHeader header) {
		super(operation, command, SCPResult.RC_ROUTE);
		this.header = header;
	}
}
