/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.scp;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.model.UnroutableMessageException;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** Represents an abstract SCP response. */
public abstract class SCPResponse {
	/** The SDP header from the response. */
	public final SDPHeader sdpHeader;

	/** The result of the SCP response. */
	public final SCPResult result;

	/** The sequence number of the SCP response, between 0 and 65535. */
	public final short sequence;

	/**
	 * Reads a packet from a bytestring of data. Subclasses must also
	 * deserialize any payload <i>after</i> calling this constructor.
	 *
	 * @param buffer
	 *            the buffer to deserialise from
	 */
	protected SCPResponse(ByteBuffer buffer) {
		assert buffer.position() == 0 : "buffer.position=" + buffer.position();
		assert buffer.order() == LITTLE_ENDIAN : "buffer.order="
				+ buffer.order();
		buffer.getShort(); // SKIP TWO PADDING BYTES
		sdpHeader = new SDPHeader(buffer, false);
		result = SCPResult.get(buffer.getShort());
		sequence = buffer.getShort();
	}

	/**
	 * Throw an exception if the response is not an {@linkplain SCPResult#RC_OK
	 * OK}.
	 *
	 * @param operation
	 *            The overall operation that was being done.
	 * @param command
	 *            The particular command that this is a response to.
	 * @throws UnexpectedResponseCodeException
	 *             If the response was a failure.
	 * @throws UnroutableMessageException
	 *             If the response was specifically that the message couldn't
	 *             be routed.
	 */
	protected final void throwIfNotOK(String operation, Enum<?> command)
			throws UnexpectedResponseCodeException {
		switch (result) {
		case RC_OK:
			return;
		case RC_ROUTE:
			throw new UnroutableMessageException(operation, command,
					sdpHeader);
		default:
			throw new UnexpectedResponseCodeException(operation, command,
					result);
		}
	}
}
