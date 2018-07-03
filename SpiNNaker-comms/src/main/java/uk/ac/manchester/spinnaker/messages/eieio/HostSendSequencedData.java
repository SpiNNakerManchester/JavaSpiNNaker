package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/**
 * Packet sent from the host to the SpiNNaker system in the context of buffering
 * input mechanism to identify packet which needs to be stored in memory for
 * future use.
 */
public class HostSendSequencedData extends EIEIOCommandMessage {
	public final byte region_id;
	public final byte sequence_no;
	public final EIEIODataMessage eieio_data_message;

	public HostSendSequencedData(byte region_id, byte sequence_no,
			EIEIODataMessage eieio_data_message) {
		super(EIEIOCommandID.HOST_SEND_SEQUENCED_DATA);
		this.region_id = region_id;
		this.sequence_no = sequence_no;
		this.eieio_data_message = eieio_data_message;
	}

	public HostSendSequencedData(EIEIOCommandHeader header, ByteBuffer data,
			int offset) {
		super(header, data, offset);
		offset += 2;
		region_id = data.get(offset++);
		sequence_no = data.get(offset++);
		eieio_data_message = EIEIOMessageFactory.read_eieio_data_message(data,
				offset);
	}

	@Override
	public int minPacketLength() {
		return 4;
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put(region_id);
		buffer.put(sequence_no);
		eieio_data_message.addToBuffer(buffer);
	}
}
