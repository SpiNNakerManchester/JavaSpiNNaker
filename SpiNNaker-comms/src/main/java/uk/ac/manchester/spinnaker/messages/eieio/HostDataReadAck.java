package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/**
 * Packet sent by the host computer to the SpiNNaker system in the context of
 * the buffering output technique to signal that the host has received a request
 * to read data.
 */
public class HostDataReadAck extends EIEIOCommandMessage {
	public final byte sequenceNumber;

	public HostDataReadAck(byte sequenceNumber) {
		super(EIEIOCommandID.HOST_DATA_READ_ACK);
		this.sequenceNumber = sequenceNumber;
	}

	public HostDataReadAck(int sequenceNumber) {
		this((byte) (sequenceNumber & 0xFF));
	}

	public HostDataReadAck(EIEIOCommandHeader header, ByteBuffer data,
			int offset) {
		super(header, data, offset);
		this.sequenceNumber = data.get(offset);
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put(sequenceNumber);
	}
}
