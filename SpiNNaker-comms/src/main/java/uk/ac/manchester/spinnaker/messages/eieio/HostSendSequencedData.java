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
	/** What region will be moved. */
	public final int regionID;
	/** The message sequence number. */
	public final int sequenceNum;
	/** The data. */
	public final EIEIODataMessage eieioDataMessage;

	/**
	 * Create a message.
	 *
	 * @param regionID
	 *            The region ID
	 * @param sequenceNum
	 *            The sequence number
	 * @param eieioDataMessage
	 *            The payload data.
	 */
	public HostSendSequencedData(byte regionID, byte sequenceNum,
			EIEIODataMessage eieioDataMessage) {
		super(HOST_SEND_SEQUENCED_DATA);
		this.regionID = regionID;
		this.sequenceNum = sequenceNum;
		this.eieioDataMessage = eieioDataMessage;
	}

	HostSendSequencedData(ByteBuffer data) {
		super(data);

		regionID = Byte.toUnsignedInt(data.get());
		sequenceNum = Byte.toUnsignedInt(data.get());
		eieioDataMessage = readDataMessage(data);
	}

	@Override
	public int minPacketLength() {
		return super.minPacketLength() + 2;
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put((byte) regionID);
		buffer.put((byte) sequenceNum);
		eieioDataMessage.addToBuffer(buffer);
	}
}
