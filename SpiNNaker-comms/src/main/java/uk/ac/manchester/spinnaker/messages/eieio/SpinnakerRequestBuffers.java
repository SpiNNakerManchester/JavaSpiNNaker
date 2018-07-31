package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Message used in the context of the buffering input mechanism which is sent by
 * the SpiNNaker system to the host computer to ask for more data to inject
 * during the simulation.
 */
public class SpinnakerRequestBuffers extends EIEIOCommandMessage implements HasCoreLocation {
	public final byte x, y, p;
	public final byte regionID, sequenceNum;
	public final int spaceAvailable;

	public SpinnakerRequestBuffers(byte x, byte y, byte p, byte regionID,
			byte sequenceNum, int spaceAvailable) {
		super(EIEIOCommandID.SPINNAKER_REQUEST_BUFFERS);
		this.x = x;
		this.y = y;
		this.p = p;
		this.regionID = regionID;
		this.sequenceNum = sequenceNum;
		this.spaceAvailable = spaceAvailable;
	}

	private static final int PROC_SHIFT = 3;
	private static final int PROC_MASK = 0b00011111;
	private static final int REGION_MASK = 0b00001111;

	public SpinnakerRequestBuffers(EIEIOCommandHeader header, ByteBuffer data) {
		super(header);
		y = data.get();
		x = data.get();
		p = (byte) ((data.get() >>> PROC_SHIFT) & PROC_MASK);
		data.get(); // ignore
		regionID = (byte) (data.get() & REGION_MASK);
		sequenceNum = data.get();
		spaceAvailable = data.getInt();
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put(y);
		buffer.put(x);
		buffer.put((byte) ((p & PROC_MASK) << PROC_SHIFT));
		buffer.put((byte) 0);
		buffer.put(regionID);
		buffer.put(sequenceNum);
		buffer.putInt(spaceAvailable);
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public int getP() {
		return p;
	}
}
