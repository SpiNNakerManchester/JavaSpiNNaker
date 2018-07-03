package uk.ac.manchester.spinnaker.messages.eieio;

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

	public HostDataRead(byte n_requests, byte sequence_no, byte[] channel,
			byte[] region_id, int[] space_read) {
		super(EIEIOCommandID.HOST_DATA_READ);
		header = new Header(n_requests, sequence_no);
		this.acks = new Ack(n_requests, channel, region_id, space_read);
	}

	public HostDataRead(byte n_requests, byte sequence_no, byte channel,
			byte region_id, int space_read) {
		super(EIEIOCommandID.HOST_DATA_READ);
		header = new Header(n_requests, sequence_no);
		this.acks = new Ack(n_requests, new byte[] { channel },
				new byte[] { region_id }, new int[] { space_read });
	}

	public HostDataRead(EIEIOCommandHeader header, ByteBuffer data, int offset) {
		super(header, data, offset);
		this.header = new Header((byte) (data.get(offset) & 0x7), data.get(offset + 1));
		offset += 2;
		byte[] channel = new byte[getNumRequests()];
		byte[] region_id = new byte[getNumRequests()];
		int[] space_read = new int[getNumRequests()];
		for (int i = 0; i < getNumRequests(); i++) {
			data.get(offset++);
			data.get(offset++);
			channel[i] = data.get(offset++);
			region_id[i] = data.get(offset++);
			space_read[i] = data.getInt(offset);
			offset += 4;
		}
		this.acks = new Ack(getNumRequests(), channel, region_id, space_read);
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
		return acks.region_id[ackID];
	}

	public int getSpaceRead(int ackID) {
		return acks.space_read[ackID];
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
		final byte[] region_id;
		final int[] space_read;

		public Ack(int n_requests, byte[] channel, byte[] region_id,
				int[] space_read) {
			if (channel.length != n_requests || region_id.length != n_requests
					|| space_read.length != n_requests) {
				throw new IllegalArgumentException(
						"lengths of channel list, region ID list, and "
								+ "space read list must all match the "
								+ "number of requests");
			}
			this.channel = channel;
			this.region_id = region_id;
			this.space_read = space_read;
		}
	}
}
