package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SequenceNumberSource.getNextSequenceNumber;

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
	private short sequence;
	private boolean sequenceSet;

	public SCPRequestHeader(SCPCommand command) {
		this.command = command;
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		buffer.putShort(command.value);
		if (!sequenceSet) {
			throw new IllegalStateException("sequence number not set");
		}
		buffer.putShort(sequence);
	}

	/**
	 * Set the sequence number of this request to the next available number.
	 * This can only ever be called once per request.
	 *
	 * @return The number that was issued.
	 */
	public short issueSequenceNumber() {
		if (sequenceSet) {
			throw new IllegalStateException(
					"a message can only have its sequence number set once");
		}
		sequence = getNextSequenceNumber();
		sequenceSet = true;
		return sequence;
	}

	public short getSequence() {
		return sequence;
	}
}
