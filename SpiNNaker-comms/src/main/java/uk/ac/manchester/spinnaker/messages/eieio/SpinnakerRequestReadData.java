package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/**
 * Message used in the context of the buffering output mechanism which is sent
 * from the SpiNNaker system to the host computer to signal that some data is
 * available to be read.
 */
public class SpinnakerRequestReadData extends EIEIOCommandMessage {
	private final Header header;
	private final Reqs reqs;

	public SpinnakerRequestReadData(byte x, byte y, byte p, byte sequence_no,
			byte n_requests, byte[] channel, byte[] region_id,
			int[] start_address, int[] space_read) {
		super(EIEIOCommandID.SPINNAKER_REQUEST_READ_DATA);
		header = new Header(x, y, p, n_requests, sequence_no);
		this.reqs = new Reqs(n_requests, channel, region_id, start_address,
				space_read);
	}

	public SpinnakerRequestReadData(byte x, byte y, byte p, byte sequence_no,
			byte n_requests, byte channel, byte region_id, int start_address,
			int space_read) {
		super(EIEIOCommandID.SPINNAKER_REQUEST_READ_DATA);
		header = new Header(x, y, p, n_requests, sequence_no);
		this.reqs = new Reqs(n_requests, new byte[] { channel },
				new byte[] { region_id }, new int[] { start_address },
				new int[] { space_read });
	}

	public SpinnakerRequestReadData(EIEIOCommandHeader header, ByteBuffer data,
			int offset) {
		super(header, data, offset);
		data.position(offset);

		byte x = data.get();
		byte y = data.get();
		byte pr = data.get();
		byte sn = data.get();
		byte p = (byte) ((pr >> 3) & 0x1F);
		byte n = (byte) (pr & 0x7);
		this.header = new Header(x, y, p, n, sn);

		byte[] channel = new byte[n];
		byte[] region_id = new byte[n];
		int[] start_address = new int[n];
		int[] space_read = new int[n];
		for (int i = 0; i < n; i++) {
			if (i != 0) {
				// Skip two bytes
				data.getShort();
			}
			channel[i] = data.get();
			region_id[i] = data.get();
			start_address[i] = (int) data.get();
			space_read[i] = (int) data.get();
		}
		this.reqs = new Reqs(n, channel, region_id, start_address, space_read);
	}

	public byte getX() {
		return header.x;
	}

	public byte getY() {
		return header.y;
	}

	public byte getP() {
		return header.p;
	}

	public byte getNumRequests() {
		return header.numRequests;
	}

	public byte getSequenceNumber() {
		return header.sequenceNumber;
	}

	public byte getChannel(int ackID) {
		return reqs.channel[ackID];
	}

	public byte getRegionID(int ackID) {
		return reqs.region_id[ackID];
	}

	public int getStartAddress(int ackID) {
		return reqs.start_address[ackID];
	}

	public int getSpaceRead(int ackID) {
		return reqs.space_read[ackID];
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put(getX());
		buffer.put(getY());
		byte n = getNumRequests();
		byte pr = (byte) (getP() << 3 | n);
		for (int i = 0; i < n; i++) {
			if (i == 0) {
				buffer.put(pr);
				buffer.put(getSequenceNumber());
			} else {
				buffer.putShort((short) 0);
			}
			buffer.put(getChannel(i));
			buffer.put(getRegionID(i));
			buffer.putInt(getStartAddress(i));
			buffer.putInt(getSpaceRead(i));
		}
	}

	/**
	 * Contains the position of the core in the machine (x, y, p), the number of
	 * requests and a sequence number.
	 */
	private static class Header {
		final byte numRequests;
		final byte sequenceNumber;
		final byte x, y, p;

		Header(byte x, byte y, byte p, byte numRequests, byte sequenceNumber) {
			this.x = x;
			this.y = y;
			this.p = p;
			this.numRequests = numRequests;
			this.sequenceNumber = sequenceNumber;
		}
	}

	/** Contains a set of requests which refer to the channels used. */
	private static class Reqs {
		final byte[] channel;
		final byte[] region_id;
		final int[] start_address;
		final int[] space_read;

		public Reqs(int n_requests, byte[] channel, byte[] region_id,
				int[] start_address, int[] space_read) {
			if (channel.length != n_requests || region_id.length != n_requests
					|| start_address.length != n_requests
					|| space_read.length != n_requests) {
				throw new IllegalArgumentException(
						"lengths of channel array, region ID array, "
								+ "start address array, and space read array "
								+ "must all match the number of requests");
			}
			this.channel = channel;
			this.region_id = region_id;
			this.start_address = start_address;
			this.space_read = space_read;
		}
	}
}
