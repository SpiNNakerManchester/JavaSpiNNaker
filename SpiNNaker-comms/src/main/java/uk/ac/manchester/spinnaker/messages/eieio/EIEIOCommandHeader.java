package uk.ac.manchester.spinnaker.messages.eieio;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/** EIEIO header for command packets. */
public class EIEIOCommandHeader implements SerializableMessage {
	/** The command ID in this header. */
	public final EIEIOCommand command;

	// Must be power of 2 (minus 1)
	private static final int MAX_COMMAND = 0x3FFF;

	/**
	 * Create a new command header.
	 *
	 * @param command
	 *            The command.
	 */
	public EIEIOCommandHeader(EIEIOCommand command) {
		this.command = requireNonNull(command, "must supply a command");
	}

	/**
	 * Create a new command header.
	 *
	 * @param command
	 *            The encoded command.
	 */
	public EIEIOCommandHeader(int command) {
		this.command = EIEIOCommandID.get(command);
	}

	/**
	 * Read an EIEIO command header from a buffer.
	 *
	 * @param buffer
	 *            The buffer to read the data from
	 */
	public EIEIOCommandHeader(ByteBuffer buffer) {
		command = EIEIOCommandID.get(buffer.getShort() & MAX_COMMAND);
	}

	private static final int FLAG1_BIT = 15;
	private static final int FLAG2_BIT = 14;

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		short value = (short) (0 << FLAG1_BIT | 1 << FLAG2_BIT | command.getValue());
		buffer.putShort(value);
	}
}
