package uk.ac.manchester.spinnaker.data_spec;

import static java.lang.Math.max;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.nio.ByteBuffer;

public class MemoryRegion {
	private int memPointer;
	private int maxWritePointer;
	private ByteBuffer buffer;
	private boolean unfilled;

	public MemoryRegion(int memoryPointer, boolean unfilled, int size) {
		memPointer = memoryPointer;
		maxWritePointer = 0;
		this.unfilled = unfilled;
		this.buffer = ByteBuffer.allocate(size).order(LITTLE_ENDIAN);
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

	public void writeIntoRegionData(byte[] data) {
		buffer.put(data);
		maxWritePointer = max(maxWritePointer, buffer.position());
	}

	public int getWritePointer() {
		return buffer.position();
	}

	public int getMaxWritePointer() {
		return maxWritePointer;
	}

	public void setWritePointer(int address) {
		buffer.position(address);
		maxWritePointer = max(maxWritePointer, address);
	}
}
