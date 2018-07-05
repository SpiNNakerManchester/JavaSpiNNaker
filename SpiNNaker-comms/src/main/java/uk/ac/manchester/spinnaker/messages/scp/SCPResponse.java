package uk.ac.manchester.spinnaker.messages.scp;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** Represents an abstract SCP BMPResponse */
public abstract class SCPResponse {
	/** The SDP header from the response */
	public final SDPHeader sdpHeader;
	/** The SCP header from the response */
	public final SCPResponseHeader scpResponseHeader;

	/**
	 * Reads a packet from a bytestring of data. Subclasses must also
	 * deserialize any payload.
	 */
	protected SCPResponse(ByteBuffer buffer) {
		sdpHeader = new SDPHeader(buffer);
		scpResponseHeader = new SCPResponseHeader(buffer);
	}
}
