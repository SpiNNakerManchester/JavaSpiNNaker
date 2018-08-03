package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.SPINNAKER_REQUEST_READ_DATA;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Message used in the context of the buffering output mechanism which is sent
 * from the SpiNNaker system to the host computer to signal that some data is
 * available to be read.
 */
public class SpinnakerRequestReadData extends EIEIOCommandMessage
		implements HasCoreLocation {
	private final int numRequests;
	private final int sequenceNumber;
	private final HasCoreLocation core;
	private final int[] channel;
	private final int[] regionID;
	private final int[] startAddress;
	private final int[] spaceRead;

	/**
	 * Create a message instance.
	 *
	 * @param core
	 *            The core talked about.
	 * @param sequenceNum
	 *            The message sequence number.
	 * @param numRequests
	 *            The expected number of requests.
	 * @param channel
	 *            The channel IDd.
	 * @param regionID
	 *            The region IDd.
	 * @param startAddress
	 *            The start addresses to read from.
	 * @param spaceRead
	 *            The number of bytes to read from each.
	 */
	public SpinnakerRequestReadData(HasCoreLocation core, int sequenceNum,
			int numRequests, int[] channel, int[] regionID,
			int[] startAddress, int[] spaceRead) {
		super(SPINNAKER_REQUEST_READ_DATA);
		this.core = core;
		this.numRequests = (byte) (numRequests & N_REQUESTS_MASK);
		this.sequenceNumber = sequenceNum;
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

	/**
	 * Create a message instance about a single move.
	 *
	 * @param core
	 *            The core talked about.
	 * @param sequenceNum
	 *            The message sequence number.
	 * @param channel
	 *            The channel ID.
	 * @param regionID
	 *            The region ID.
	 * @param startAddress
	 *            The start address to read from.
	 * @param spaceRead
	 *            The number of bytes to read.
	 */
	public SpinnakerRequestReadData(HasCoreLocation core, int sequenceNum,
			int channel, int regionID, int startAddress, int spaceRead) {
		this(core, sequenceNum, (byte) 1, new int[] {
				channel
		}, new int[] {
				regionID
		}, new int[] {
				startAddress
		}, new int[] {
				spaceRead
		});
	}

	private static final int CORE_SHIFT = 3;
	private static final int N_REQUESTS_MASK = (1 << CORE_SHIFT) - 1;

	/**
	 * Deserialise.
	 *
	 * @param data
	 *            the data buffer.
	 */
	SpinnakerRequestReadData(ByteBuffer data) {
		super(data);

		int x = Byte.toUnsignedInt(data.get());
		int y = Byte.toUnsignedInt(data.get());
		int pr = Byte.toUnsignedInt(data.get());
		this.sequenceNumber = Byte.toUnsignedInt(data.get());

		byte p = (byte) (pr >>> CORE_SHIFT);
		int n = pr & N_REQUESTS_MASK;
		this.core = new CoreLocation(x, y, p);
		this.numRequests = (byte) n;

		channel = new int[n];
		regionID = new int[n];
		startAddress = new int[n];
		spaceRead = new int[n];
		for (int i = 0; i < n; i++) {
			if (i != 0) {
				// Skip two bytes
				data.getShort();
			}
			channel[i] = Byte.toUnsignedInt(data.get());
			regionID[i] = Byte.toUnsignedInt(data.get());
			startAddress[i] = data.getInt();
			spaceRead[i] = data.getInt();
		}
	}

	@Override
	public int getX() {
		return core.getX();
	}

	@Override
	public int getY() {
		return core.getY();
	}

	@Override
	public int getP() {
		return core.getP();
	}

	public int getNumRequests() {
		return numRequests;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public int getChannel(int ackID) {
		return channel[ackID];
	}

	public int getRegionID(int ackID) {
		return regionID[ackID];
	}

	public int getStartAddress(int ackID) {
		return startAddress[ackID];
	}

	public int getSpaceRead(int ackID) {
		return spaceRead[ackID];
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put((byte) core.getX());
		buffer.put((byte) core.getY());

		for (int i = 0; i < numRequests; i++) {
			if (i == 0) {
				buffer.put((byte) (core.getP() << CORE_SHIFT | numRequests));
				buffer.put((byte) sequenceNumber);
			} else {
				buffer.putShort((short) 0);
			}

			buffer.put((byte) channel[i]);
			buffer.put((byte) regionID[i]);
			buffer.putInt(startAddress[i]);
			buffer.putInt(spaceRead[i]);
		}
	}
}
