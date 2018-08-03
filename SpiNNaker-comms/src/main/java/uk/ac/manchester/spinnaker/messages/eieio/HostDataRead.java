package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.HOST_DATA_READ;

import java.nio.ByteBuffer;

/**
 * Packet sent by the host computer to the SpiNNaker system in the context of
 * the buffering output technique to signal that the host has completed reading
 * data from the output buffer, and that such space can be considered free to
 * use again.
 *
 * @see HostDataReadAck
 */
public class HostDataRead extends EIEIOCommandMessage {
	private final Header header;
	private final Ack acks;

	/**
	 * Create.
	 *
	 * @param numRequests
	 *            The number of requests we are talking about. This is used to
	 *            check the validity of other arguments.
	 * @param sequenceNum
	 *            The message sequence number.
	 * @param channel
	 *            What channels are we talking about.
	 * @param regionID
	 *            What regions are we talking about.
	 * @param spaceRead
	 *            How much space has been read from each region.
	 */
	public HostDataRead(byte numRequests, byte sequenceNum, byte[] channel,
			byte[] regionID, int[] spaceRead) {
		super(HOST_DATA_READ);
		header = new Header(numRequests, sequenceNum);
		this.acks = new Ack(numRequests, channel, regionID, spaceRead);
	}

	/**
	 * Create.
	 *
	 * @param sequenceNum
	 *            The message sequence number.
	 * @param channel
	 *            What channel are we talking about.
	 * @param regionID
	 *            What region are we talking about.
	 * @param spaceRead
	 *            How much space has been read.
	 */
	public HostDataRead(byte sequenceNum, byte channel, byte regionID,
			int spaceRead) {
		this((byte) 1, sequenceNum, new byte[] {
				channel
		}, new byte[] {
				regionID
		}, new int[] {
				spaceRead
		});
	}

	private static final int NUM_REQUESTS_MASK = 0b00000111;

	/**
	 * Deseralise.
	 *
	 * @param data
	 *            what to deserialise from
	 */
	HostDataRead(ByteBuffer data) {
		super(data);
		this.header =
				new Header((byte) (data.get() & NUM_REQUESTS_MASK), data.get());
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

	public int getNumRequests() {
		return Byte.toUnsignedInt(header.numRequests);
	}

	public int getSequenceNumber() {
		return Byte.toUnsignedInt(header.sequenceNumber);
	}

	public int getChannel(int ackID) {
		return Byte.toUnsignedInt(acks.channel[ackID]);
	}

	public int getRegionID(int ackID) {
		return Byte.toUnsignedInt(acks.regionID[ackID]);
	}

	public int getSpaceRead(int ackID) {
		return acks.spaceRead[ackID];
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put(header.numRequests);
		buffer.put(header.sequenceNumber);
		for (int i = 0; i < header.numRequests; i++) {
			buffer.putShort((short) 0);
			buffer.put(acks.channel[i]);
			buffer.put(acks.regionID[i]);
			buffer.putInt(acks.spaceRead[i]);
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

		Ack(int numRequests, byte[] channel, byte[] regionID, int[] spaceRead) {
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
