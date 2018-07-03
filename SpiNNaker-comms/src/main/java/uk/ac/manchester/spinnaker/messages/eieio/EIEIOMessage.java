package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/** Marker interface for an EIEIO message. */
public abstract interface EIEIOMessage {
	/**
	 * Writes this message into the given buffer. This is so that a message can
	 * be sent.
	 *
	 * @param buffer
	 *            The buffer to write into.
	 */
	void addToBuffer(ByteBuffer buffer);
}
