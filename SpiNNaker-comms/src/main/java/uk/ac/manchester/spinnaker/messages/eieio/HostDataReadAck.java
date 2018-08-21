package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.HOST_DATA_READ_ACK;

import java.nio.ByteBuffer;

/**
 * Packet sent by the host computer to the SpiNNaker system in the context of
 * the buffering output technique to signal that the host has received a request
 * to read data.
 *
 * @see HostDataRead
 */
public class HostDataReadAck extends EIEIOCommandMessage {
	/** The message sequence number that is being acknowledged. */
	public final byte sequenceNumber;

	/**
	 * Create.
	 *
	 * @param sequenceNumber
	 *            The message sequence number that is being acknowledged.
	 */
	public HostDataReadAck(byte sequenceNumber) {
		super(HOST_DATA_READ_ACK);
		this.sequenceNumber = sequenceNumber;
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put(sequenceNumber);
	}
}
