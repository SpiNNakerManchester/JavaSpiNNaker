package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.HOST_DATA_READ;

import java.nio.ByteBuffer;

/**
 * Packet sent by the host computer to the SpiNNaker system in the context of
 * the buffering output technique to signal that the host has completed reading
 * data from the output buffer, and that such space can be considered free to
 * use again.
 */
public class HostDataRead extends EIEIOCommandMessage {
	private final Header header;
	private final Ack acks;

	public HostDataRead(byte numRequests, byte sequenceNum, byte[] channel,
			byte[] regionID, int[] spaceRead) {
		super(HOST_DATA_READ);
		header = new Header(numRequests, sequenceNum);
		this.acks = new Ack(numRequests, channel, regionID, spaceRead);
	}

	public HostDataRead(byte numRequests, byte sequenceNum, byte channel,
			byte regionID, int spaceRead) {
		super(HOST_DATA_READ);
		header = new Header(numRequests, sequenceNum);
		this.acks = new Ack(numRequests, new byte[] {
				channel
		}, new byte[] {
				regionID
		}, new int[] {
				spaceRead
		});
	}

	HostDataRead(EIEIOCommandHeader header, ByteBuffer data) {
		super(header);
		this.header = new Header((byte) (data.get() & 0x7), data.get());
		byte[] channel = new byte[getNumRequests()];
		byte[] regionID = new byte[getNumRequests()];
		int[] spaceRead = new int[getNumRequests()];
		for (int i = 0; i < getNumRequests(); i++) {
			data.get();
			data.get();
			channel[i] = data.get();
			regionID[i] = data.get();
			spaceRead[i] = data.getInt();
		}
		this.acks = new Ack(getNumRequests(), channel, regionID, spaceRead);
	}

	public byte getNumRequests() {
		return header.numRequests;
	}

	public byte getSequenceNumber() {
		return header.sequenceNumber;
	}

	public byte getChannel(int ackID) {
		return acks.channel[ackID];
	}

	public byte getRegionID(int ackID) {
		return acks.regionID[ackID];
	}

	public int getSpaceRead(int ackID) {
		return acks.spaceRead[ackID];
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put(getNumRequests());
		buffer.put(getSequenceNumber());
		for (int i = 0; i < getNumRequests(); i++) {
			buffer.putShort((short) 0);
			buffer.put(getChannel(i));
			buffer.put(getRegionID(i));
			buffer.putInt(getSpaceRead(i));
		}
	}

	/**
	 * The HostDataRead contains itself on header with the number of requests
	 * and a sequence number.
	 */
	private static class Header {
		final byte numRequests;
		final byte sequenceNumber;

		Header(byte numRequests, byte sequenceNumber) {
			this.numRequests = numRequests;
			this.sequenceNumber = sequenceNumber;
		}
	}

	/** Contains a set of acks which refer to each of the channels read. */
	private static class Ack {
		final byte[] channel;
		final byte[] regionID;
		final int[] spaceRead;

		public Ack(int numRequests, byte[] channel, byte[] regionID,
				int[] spaceRead) {
			if (channel.length != numRequests || regionID.length != numRequests
					|| spaceRead.length != numRequests) {
				throw new IllegalArgumentException(
						"lengths of channel list, region ID list, and "
								+ "space read list must all match the "
								+ "number of requests");
			}
			this.channel = channel;
			this.regionID = regionID;
			this.spaceRead = spaceRead;
		}
	}
}
