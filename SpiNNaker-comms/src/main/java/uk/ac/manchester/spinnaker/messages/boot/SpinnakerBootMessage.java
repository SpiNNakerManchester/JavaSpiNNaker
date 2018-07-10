package uk.ac.manchester.spinnaker.messages.boot;

import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/** A message used for booting the board */
public class SpinnakerBootMessage implements SerializableMessage {
	private static final short BOOT_MESSAGE_VERSION = 1;
	/** The payload data (or <tt>null</tt> if there is none) */
	public final ByteBuffer data;
	/** The operation of this packet */
	public final SpinnakerBootOpCode opcode;
	/** The first operand */
	public final int operand1;
	/** The second operand */
	public final int operand2;
	/** The third operand */
	public final int operand3;

	public SpinnakerBootMessage(SpinnakerBootOpCode opcode, int operand_1,
			int operand_2, int operand_3) {
		this.opcode = opcode;
		this.operand1 = operand_1;
		this.operand2 = operand_2;
		this.operand3 = operand_3;
		this.data = null;
	}

	public SpinnakerBootMessage(SpinnakerBootOpCode opcode, int operand_1,
			int operand_2, int operand_3, ByteBuffer buffer) {
		this.opcode = opcode;
		this.operand1 = operand_1;
		this.operand2 = operand_2;
		this.operand3 = operand_3;
		this.data = buffer.asReadOnlyBuffer();
		if (data.remaining() > 256 * 4) {
			throw new IllegalArgumentException(
					"A boot packet can contain at most 256 words of data");
		}
	}

	public SpinnakerBootMessage(SpinnakerBootOpCode opcode, int operand_1,
			int operand_2, int operand_3, byte[] bytes) {
		this.opcode = opcode;
		this.operand1 = operand_1;
		this.operand2 = operand_2;
		this.operand3 = operand_3;
		this.data = wrap(bytes).asReadOnlyBuffer();
		if (data.remaining() > 256 * 4) {
			throw new IllegalArgumentException(
					"A boot packet can contain at most 256 words of data");
		}
	}

	/** Deserialise a boot message from a received message. */
	public SpinnakerBootMessage(ByteBuffer buffer) {
		buffer.getShort(); // TODO check message version?
		opcode = SpinnakerBootOpCode.get(buffer.getInt());
		operand1 = buffer.getInt();
		operand2 = buffer.getInt();
		operand3 = buffer.getInt();
		if (buffer.hasRemaining()) {
			data = wrap(buffer.array(), buffer.position(), buffer.remaining())
					.order(LITTLE_ENDIAN).asReadOnlyBuffer();
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
