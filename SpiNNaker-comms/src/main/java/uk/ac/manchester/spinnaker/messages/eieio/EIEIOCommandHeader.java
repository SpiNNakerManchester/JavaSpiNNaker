package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/** EIEIO header for command packets */
public class EIEIOCommandHeader {
	public final EIEIOCommandID command;

	// Must be power of 2
	private static final int MAX_COMMAND = 0x3FFF;

	public EIEIOCommandHeader(EIEIOCommandID command) {
		this(command.getValue());
	}

	public EIEIOCommandHeader(int command) {
		if (command < 0 || command > MAX_COMMAND) {
			throw new IllegalArgumentException(
					"parameter command is outside the allowed range (0 to "
							+ MAX_COMMAND + ")");
		}
		this.command = EIEIOCommandID.get(command);
	}

	/**
	 * Read an EIEIO command header from a buffer
	 *
	 * @param buffer
	 *            The buffer to read the data from
	 * @param offset
	 *            The offset where the valid data starts
	 */
	public EIEIOCommandHeader(ByteBuffer buffer, int offset) {
		command = EIEIOCommandID.get(buffer.getShort(offset) & MAX_COMMAND);
	}

	/**
	 * Add the bytestring of the header to the buffer.
	 *
	 * @param buffer
	 *            The buffer to append to.
	 */
	public void addToBuffer(ByteBuffer buffer) {
		short value = (short) (0 << 15 | 1 << 14 | command.getValue());
		buffer.putShort(value);
	}
}
