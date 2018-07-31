package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.SPINNAKER_REQUEST_READ_DATA;

import java.nio.ByteBuffer;

/**
 * Message used in the context of the buffering output mechanism which is sent
 * from the SpiNNaker system to the host computer to signal that some data is
 * available to be read.
 */
public class SpinnakerRequestReadData extends EIEIOCommandMessage {
	private final Header header;
	private final Reqs reqs;

	public SpinnakerRequestReadData(byte x, byte y, byte p, byte sequenceNum,
			byte numRequests, byte[] channel, byte[] regionID,
			int[] startAddress, int[] spaceRead) {
		super(SPINNAKER_REQUEST_READ_DATA);
		header = new Header(x, y, p, numRequests, sequenceNum);
		this.reqs = new Reqs(numRequests, channel, regionID, startAddress,
				spaceRead);
	}

	public SpinnakerRequestReadData(byte x, byte y, byte p, byte sequenceNum,
			byte numRequests, byte channel, byte regionID, int startAddress,
			int spaceRead) {
		super(SPINNAKER_REQUEST_READ_DATA);
		header = new Header(x, y, p, numRequests, sequenceNum);
		this.reqs = new Reqs(numRequests, new byte[] {
				channel
		}, new byte[] {
				regionID
		}, new int[] {
				startAddress
		}, new int[] {
				spaceRead
		});
	}

	public SpinnakerRequestReadData(EIEIOCommandHeader header,
			ByteBuffer data) {
		super(header);

		byte x = data.get();
		byte y = data.get();
		byte pr = data.get();
		byte sn = data.get();
		byte p = (byte) ((pr >> 3) & 0x1F);
		byte n = (byte) (pr & 0x7);
		this.header = new Header(x, y, p, n, sn);

		byte[] channel = new byte[n];
		byte[] regionID = new byte[n];
		int[] startAddress = new int[n];
		int[] spaceRead = new int[n];
		for (int i = 0; i < n; i++) {
			if (i != 0) {
				// Skip two bytes
				data.getShort();
			}
			channel[i] = data.get();
			regionID[i] = data.get();
			startAddress[i] = (int) data.get();
			spaceRead[i] = (int) data.get();
		}
		this.reqs = new Reqs(n, channel, regionID, startAddress, spaceRead);
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
		return reqs.regionID[ackID];
	}

	public int getStartAddress(int ackID) {
		return reqs.startAddress[ackID];
	}

	public int getSpaceRead(int ackID) {
		return reqs.spaceRead[ackID];
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
		final byte[] regionID;
		final int[] startAddress;
		final int[] spaceRead;

		Reqs(int numRequests, byte[] channel, byte[] regionID,
				int[] startAddress, int[] spaceRead) {
			if (channel.length != numRequests || regionID.length != numRequests
					|| startAddress.length != numRequests
					|| spaceRead.length != numRequests) {
				throw new IllegalArgumentException(
						"lengths of channel array, region ID array, "
								+ "start address array, and space read array "
								+ "must all match the number of requests");
			}
			this.channel = channel;
			this.regionID = regionID;
			this.startAddress = startAddress;
			this.spaceRead = spaceRead;
		}
	}
}
