package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.HOST_SEND_SEQUENCED_DATA;
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessageFactory.readDataMessage;

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
		super(HOST_SEND_SEQUENCED_DATA);
		this.regionID = regionID;
		this.sequenceNum = sequenceNum;
		this.eieioDataMessage = eieioDataMessage;
	}

	public HostSendSequencedData(EIEIOCommandHeader header, ByteBuffer data) {
		super(header);
		regionID = data.get();
		sequenceNum = data.get();
		eieioDataMessage = readDataMessage(data);
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
