package uk.ac.manchester.spinnaker.messages.scp;

import java.nio.ByteBuffer;

/** Represents the header of an SCP response. */
public final class SCPResponseHeader {
	/** The result of the SCP response. */
	public final SCPResult result;
	/** The sequence number of the SCP response, between 0 and 65535. */
	public final short sequence;

	public SCPResponseHeader(ByteBuffer buffer) {
		result = SCPResult.get(buffer.getShort());
		sequence = buffer.getShort();
	}
}
