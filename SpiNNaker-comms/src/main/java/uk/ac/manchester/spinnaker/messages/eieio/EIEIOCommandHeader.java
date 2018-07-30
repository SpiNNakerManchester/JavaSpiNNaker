package uk.ac.manchester.spinnaker.messages.eieio;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/** EIEIO header for command packets. */
public class EIEIOCommandHeader implements SerializableMessage {
	public final EIEIOCommandID command;

	// Must be power of 2 (minus 1)
	private static final int MAX_COMMAND = 0x3FFF;

	public EIEIOCommandHeader(EIEIOCommandID command) {
		this.command = requireNonNull(command, "must supply a command");
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
	 * Read an EIEIO command header from a buffer.
	 *
	 * @param buffer
	 *            The buffer to read the data from
	 * @param offset
	 *            The offset where the valid data starts
	 */
	public EIEIOCommandHeader(ByteBuffer buffer, int offset) {
		command = EIEIOCommandID.get(buffer.getShort(offset) & MAX_COMMAND);
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		short value = (short) (0 << 15 | 1 << 14 | command.getValue());
		buffer.putShort(value);
	}
}
