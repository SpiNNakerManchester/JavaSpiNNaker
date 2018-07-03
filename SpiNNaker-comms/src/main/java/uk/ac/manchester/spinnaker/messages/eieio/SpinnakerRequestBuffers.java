package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/**
 * Message used in the context of the buffering input mechanism which is sent by
 * the SpiNNaker system to the host computer to ask for more data to inject
 * during the simulation.
 */
public class SpinnakerRequestBuffers extends EIEIOCommandMessage {
	public final byte x, y, p;
	public final byte region_id, sequence_no;
	public final int space_available;

	public SpinnakerRequestBuffers(byte x, byte y, byte p, byte region_id,
			byte sequence_no, int space_available) {
		super(EIEIOCommandID.SPINNAKER_REQUEST_BUFFERS);
		this.x = x;
		this.y = y;
		this.p = p;
		this.region_id = region_id;
		this.sequence_no = sequence_no;
		this.space_available = space_available;
	}

	public SpinnakerRequestBuffers(EIEIOCommandHeader header, ByteBuffer data,
			int offset) {
		super(header, data, offset);
		offset += 2;
		y = data.get(offset++);
		x = data.get(offset++);
		p = (byte) ((data.get(offset++) >> 3) & 0x1F);
		offset++;
		region_id = (byte) (data.get(offset++) & 0xF);
		sequence_no = data.get(offset++);
		space_available = data.getInt(offset);
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put(y);
		buffer.put(x);
		buffer.put((byte) (p << 3));
		buffer.put((byte) 0);
		buffer.put(region_id);
		buffer.put(sequence_no);
		buffer.putInt(space_available);
	}
}
