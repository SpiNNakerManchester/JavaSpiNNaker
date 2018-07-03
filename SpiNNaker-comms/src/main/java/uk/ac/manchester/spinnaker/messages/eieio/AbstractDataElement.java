package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/**
 * A marker interface for possible data elements in the EIEIO data packet
 */
public abstract class AbstractDataElement {

	/**
	 * Writes this message chunk into the given buffer. This is so that a
	 * message can be sent.
	 *
	 * @param buffer
	 *            The buffer to write into.
	 * @param eieioType
	 *            The type of message this is being written into.
	 * @throws IllegalArgumentException
	 *             If this message is incompatible with the given message type.
	 */
	public abstract void addToBuffer(ByteBuffer elements, EIEIOType eieioType);
}
