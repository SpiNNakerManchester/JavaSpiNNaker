/*
 * Copyright (c) 2022-2023 The University of Manchester
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
