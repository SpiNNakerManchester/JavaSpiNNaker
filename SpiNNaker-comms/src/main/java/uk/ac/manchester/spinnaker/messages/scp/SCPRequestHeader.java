package uk.ac.manchester.spinnaker.messages.scp;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/**
 * Represents the header of an SCP Request.
 * <p>
 * The sequence number, if zero, will be set by the message sending code to the
 * actual sequence number when the message is sent on a connection.
 */
public class SCPRequestHeader implements SerializableMessage {
	/** The command of the SCP packet. */
	public final SCPCommand command;
	/** The sequence number of the packet, between 0 and 65535. */
	public short sequence;

	public SCPRequestHeader(SCPCommand command) {
		this(command, 0);
	}

	public SCPRequestHeader(SCPCommand command, int sequence) {
		this.command = command;
		this.sequence = (short) sequence;
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		buffer.putShort(command.value);
		buffer.putShort(sequence);
	}
}
