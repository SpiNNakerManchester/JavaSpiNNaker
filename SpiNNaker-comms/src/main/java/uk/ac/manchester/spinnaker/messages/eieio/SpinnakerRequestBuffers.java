package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/**
 * Message used in the context of the buffering input mechanism which is sent by
 * the SpiNNaker system to the host computer to ask for more data to inject
 * during the simulation.
 */
public class SpinnakerRequestBuffers extends EIEIOCommandMessage {
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

	public SpinnakerRequestBuffers(EIEIOCommandHeader header, ByteBuffer data,
			int offset) {
		super(header, data, offset);
		offset += 2;
		y = data.get(offset++);
		x = data.get(offset++);
		p = (byte) ((data.get(offset++) >> 3) & 0x1F);
		offset++;
		regionID = (byte) (data.get(offset++) & 0xF);
		sequenceNum = data.get(offset++);
		spaceAvailable = data.getInt(offset);
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put(y);
		buffer.put(x);
		buffer.put((byte) (p << 3));
		buffer.put((byte) 0);
		buffer.put(regionID);
		buffer.put(sequenceNum);
		buffer.putInt(spaceAvailable);
	}
}
