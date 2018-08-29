package uk.ac.manchester.spinnaker.data_spec;

import java.nio.ByteBuffer;

public class MemoryRegion {
	private int memPointer;
	private ByteBuffer buffer;
	private boolean unfilled;

	public MemoryRegion(int memoryPointer, boolean unfilled, int size) {
		memPointer = memoryPointer;
		this.unfilled = unfilled;
		this.buffer = ByteBuffer.allocate(size);
	}

	public int getMemoryPointer() {
		return memPointer;
	}

	public int getAllocatedSize() {
		return buffer.capacity();
	}

	public int getRemainingSpace() {
		return buffer.remaining();
	}

	public boolean isUnfilled() {
		return unfilled;
	}

	public ByteBuffer getRegionData() {
		return buffer;
	}

	public int getWritePointer() {
		return buffer.position();
	}

	public void setWritePointer(int address) {
		buffer.position(address);
	}
}
