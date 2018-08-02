package uk.ac.manchester.spinnaker.messages.eieio;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;

/**
 * An EIEIO message containing a command.
 *
 * @author Sergio Davies
 * @author Donal Fellows
 */
public class EIEIOCommandMessage
		implements EIEIOMessage<EIEIOCommandMessage.Header> {
	// Must be power of 2 (minus 1)
	private static final int MAX_COMMAND = 0x3FFF;
	private static final int FLAG1_BIT = 15;
	private static final int FLAG2_BIT = 14;

	/** The header of the message. */
	private final Header header;

	protected EIEIOCommandMessage(ByteBuffer buffer) {
		this.header = new Header(buffer);
	}

	public EIEIOCommandMessage(EIEIOCommand command) {
		this.header = new Header(command);
	}

	static EIEIOCommand peekCommand(ByteBuffer buffer) {
		return EIEIOCommandID.get(buffer.getShort(0) & MAX_COMMAND);
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		header.addToBuffer(buffer);
	}

	@Override
	public int minPacketLength() {
		return 2;
	}

	@Override
	public Header getHeader() {
		return header;
	}

	/** EIEIO header for command packets. */
	public static class Header implements EIEIOHeader {
		/** The command ID in this header. */
		public final EIEIOCommand command;

		/**
		 * Create a new command header.
		 *
		 * @param command
		 *            The command.
		 */
		public Header(EIEIOCommand command) {
			this.command = requireNonNull(command, "must supply a command");
		}

		/**
		 * Create a new command header.
		 *
		 * @param command
		 *            The encoded command.
		 */
		public Header(int command) {
			this.command = EIEIOCommandID.get(command);
		}

		/**
		 * Read an EIEIO command header from a buffer.
		 *
		 * @param buffer
		 *            The buffer to read the data from
		 */
		private Header(ByteBuffer buffer) {
			command = EIEIOCommandID.get(buffer.getShort() & MAX_COMMAND);
		}

		@Override
		public void addToBuffer(ByteBuffer buffer) {
			short value = (short) (0 << FLAG1_BIT | 1 << FLAG2_BIT
					| command.getValue());
			buffer.putShort(value);
		}
	}
}
