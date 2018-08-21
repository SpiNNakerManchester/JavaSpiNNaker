package uk.ac.manchester.spinnaker.messages;

import java.nio.ByteBuffer;

/**
 * Represents a serializable message or a piece of a serializable message.
 * <p>
 * Concrete classes that implement this interface that also wish to be
 * deserializable should also support the reverse operation by creating a
 * constructor that takes a single (little-endian) ByteBuffer as its only
 * argument.
 */
public interface SerializableMessage {
	/**
	 * Writes this message into the given buffer as a contiguous range of bytes.
	 * This is so that a message can be sent. Implementors may assume that the
	 * buffer has been configured to be
	 * {@linkplain java.nio.ByteOrder#LITTLE_ENDIAN little-endian} and that its
	 * <i>position</i> is at the point where they should begin writing. Once it
	 * has finished, the <i>position</i> should be immediately after the last
	 * byte written by this method.
	 *
	 * @param buffer
	 *            The buffer to write into.
	 */
	void addToBuffer(ByteBuffer buffer);
}
