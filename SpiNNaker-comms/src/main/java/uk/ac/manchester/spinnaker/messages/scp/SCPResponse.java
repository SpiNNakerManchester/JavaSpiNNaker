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
	 * deserialize any payload <i>after</i> calling this constructor.
	 *
	 * @param buffer
	 *            the buffer to deserialise from
	 */
	protected SCPResponse(ByteBuffer buffer) {
		assert buffer.position() == 0;
		assert buffer.order() == LITTLE_ENDIAN;
		sdpHeader = new SDPHeader(buffer);
		scpResponseHeader = new SCPResponseHeader(buffer);
	}

	/** Represents the header of an SCP response. */
	static final class SCPResponseHeader {
		/** The result of the SCP response. */
		public final SCPResult result;
		/** The sequence number of the SCP response, between 0 and 65535. */
		public final short sequence;

		SCPResponseHeader(ByteBuffer buffer) {
			result = SCPResult.get(buffer.getShort());
			sequence = buffer.getShort();
		}
	}
}
