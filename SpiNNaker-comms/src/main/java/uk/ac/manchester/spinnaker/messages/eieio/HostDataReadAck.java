package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/**
 * Packet sent by the host computer to the SpiNNaker system in the context of
 * the buffering output technique to signal that the host has received a request
 * to read data.
 */
public class HostDataReadAck extends EIEIOCommandMessage {
	public final byte sequence_number;

	public HostDataReadAck(byte sequence_number) {
		super(EIEIOCommandID.HOST_DATA_READ_ACK);
		this.sequence_number = sequence_number;
	}

	public HostDataReadAck(int sequence_number) {
		this((byte) (sequence_number & 0xFF));
	}

	public HostDataReadAck(EIEIOCommandHeader header, ByteBuffer data,
			int offset) {
		super(header, data, offset);
		this.sequence_number = data.get(offset);
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put(sequence_number);
	}
}
