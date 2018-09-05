package uk.ac.manchester.spinnaker.messages.boot;

import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/** A message used for booting the board. */
public class BootMessage implements SerializableMessage {
	private static final short BOOT_MESSAGE_VERSION = 1;
	private static final int BOOT_PACKET_SIZE = 256 * 4;
	/** The payload data (or <tt>null</tt> if there is none). */
	public final ByteBuffer data;
	/** The operation of this packet. */
	public final BootOpCode opcode;
	/** The first operand. */
	public final int operand1;
	/** The second operand. */
	public final int operand2;
	/** The third operand. */
	public final int operand3;

	/**
	 * Create a boot message.
	 *
	 * @param opcode
	 *            The boot opcode
	 * @param operand1
	 *            The first arg
	 * @param operand2
	 *            The second arg
	 * @param operand3
	 *            The third arg
	 */
	public BootMessage(BootOpCode opcode, int operand1,
			int operand2, int operand3) {
		this.opcode = opcode;
		this.operand1 = operand1;
		this.operand2 = operand2;
		this.operand3 = operand3;
		this.data = null;
	}

	/**
	 * Create a boot message.
	 *
	 * @param opcode
	 *            The boot opcode
	 * @param operand1
	 *            The first arg
	 * @param operand2
	 *            The second arg
	 * @param operand3
	 *            The third arg
	 * @param buffer
	 *            The payload
	 */
	public BootMessage(BootOpCode opcode, int operand1,
			int operand2, int operand3, ByteBuffer buffer) {
		this.opcode = opcode;
		this.operand1 = operand1;
		this.operand2 = operand2;
		this.operand3 = operand3;
		this.data = buffer.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		if (data.remaining() > BOOT_PACKET_SIZE) {
			throw new IllegalArgumentException(
					"A boot packet can contain at most 256 words of data");
		}
	}

	/**
	 * Deserialise a boot message from a received message.
	 *
	 * @param buffer
	 *            the buffer to read out of.
	 */
	public BootMessage(ByteBuffer buffer) {
		buffer.getShort(); // TODO check message version?
		opcode = BootOpCode.get(buffer.getInt());
		operand1 = buffer.getInt();
		operand2 = buffer.getInt();
		operand3 = buffer.getInt();
		if (buffer.hasRemaining()) {
			data = wrap(buffer.array(), buffer.position(), buffer.remaining())
					.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		} else {
			data = null;
		}
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		buffer.putShort(BOOT_MESSAGE_VERSION);
		buffer.putInt(opcode.value);
		buffer.putInt(operand1);
		buffer.putInt(operand2);
		buffer.putInt(operand3);
		if (data != null) {
			buffer.put(data);
		}
	}
}
