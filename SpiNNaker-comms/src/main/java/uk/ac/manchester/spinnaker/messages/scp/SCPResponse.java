package uk.ac.manchester.spinnaker.messages.scp;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** Represents an abstract SCP response. */
public abstract class SCPResponse {
	/** The SDP header from the response. */
	public final SDPHeader sdpHeader;
	/** The SCP header from the response. */
	public final SCPResponseHeader scpResponseHeader;

	/**
	 * Reads a packet from a bytestring of data. Subclasses must also
	 * deserialize any payload.
	 */
	protected SCPResponse(ByteBuffer buffer) {
		assert buffer.position() == 0;
		assert buffer.order() == LITTLE_ENDIAN;
		sdpHeader = new SDPHeader(buffer);
		scpResponseHeader = new SCPResponseHeader(buffer);
	}
}
