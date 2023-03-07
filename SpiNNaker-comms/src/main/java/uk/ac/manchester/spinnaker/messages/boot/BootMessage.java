/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.boot;

import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/**
 * A message used for booting the board. Note that boot messages are big endian,
 * unlike the rest of SpiNNaker.
 */
public class BootMessage implements SerializableMessage {
	private static final short BOOT_MESSAGE_VERSION = 1;

	private static final int BOOT_PACKET_SIZE = 256 * WORD_SIZE;

	/** The payload data (or {@code null} if there is none). */
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
	public BootMessage(BootOpCode opcode, int operand1, int operand2,
			int operand3) {
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
	 * @throws IllegalArgumentException
	 *             if the payload is too large for the message
	 */
	public BootMessage(BootOpCode opcode, int operand1, int operand2,
			int operand3, ByteBuffer buffer) {
		this.opcode = opcode;
		this.operand1 = operand1;
		this.operand2 = operand2;
		this.operand3 = operand3;
		this.data = buffer.duplicate();
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
			data = buffer.asReadOnlyBuffer();
		} else {
			data = null;
		}
	}

	/**
	 * Writes this message into the given buffer as a contiguous range of bytes.
	 * This is so that a message can be sent. Implementors may assume that the
	 * buffer has been configured to be <strong>big</strong>-endian and that its
	 * position is at the point where they should begin writing. Once it has
	 * finished, the position should be immediately after the last byte written
	 * by this method.
	 */
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
