package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/**
 * Packet sent from the host to the SpiNNaker system in the context of buffering
 * input mechanism to identify packet which needs to be stored in memory for
 * future use.
 */
public class HostSendSequencedData extends EIEIOCommandMessage {
	public final byte regionID;
	public final byte sequenceNum;
	public final EIEIODataMessage eieioDataMessage;

	public HostSendSequencedData(byte regionID, byte sequenceNum,
			EIEIODataMessage eieioDataMessage) {
		super(EIEIOCommandID.HOST_SEND_SEQUENCED_DATA);
		this.regionID = regionID;
		this.sequenceNum = sequenceNum;
		this.eieioDataMessage = eieioDataMessage;
	}

	public HostSendSequencedData(EIEIOCommandHeader header, ByteBuffer data,
			int offset) {
		super(header, data, offset);
		offset += 2;
		regionID = data.get(offset++);
		sequenceNum = data.get(offset++);
		eieioDataMessage = EIEIOMessageFactory.readDataMessage(data,
				offset);
	}

	@Override
	public int minPacketLength() {
		return 4;
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put(regionID);
		buffer.put(sequenceNum);
		eieioDataMessage.addToBuffer(buffer);
	}
}
