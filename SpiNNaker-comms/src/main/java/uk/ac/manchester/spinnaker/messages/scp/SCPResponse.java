package uk.ac.manchester.spinnaker.messages.scp;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_OK;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
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
		assert buffer.position() == 0;
		assert buffer.order() == LITTLE_ENDIAN;
		sdpHeader = new SDPHeader(buffer);
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
	 */
	protected final void throwIfNotOK(String operation, SCPCommand command)
			throws UnexpectedResponseCodeException {
		if (result != RC_OK) {
			throw new UnexpectedResponseCodeException(operation, command,
					result);
		}
	}
}
