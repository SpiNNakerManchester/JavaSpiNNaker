package uk.ac.manchester.spinnaker.data_spec;

import static java.lang.Math.max;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.nio.ByteBuffer;

/**
 * A memory region storage object.
 * <p>
 * As memory regions are modifiable objects, their equality is defined in terms
 * of their fundamental object identity.
 */
public class MemoryRegion {
	/** The write pointer position, saying where the start of the block is. */
	private int memPointer;
	/** The maximum point where writes have happened up to within the region. */
	private int maxWritePointer;
	/** The buffer storing the written data. */
	private ByteBuffer buffer;
	/**
	 * Whether this is an unfilled region. Unfilled regions can be written
	 * efficiently as a block of zeroes.
	 */
	private boolean unfilled;
	/** The base address of the region. Set after the fact. */
	private int regionBaseAddress;

	/**
	 * Create a memory region.
	 *
	 * @param memoryPointer
	 *            where the start of the block is
	 * @param unfilled
	 *            whether this is an unfilled region
	 * @param size
	 *            the allocated size of the memory region
	 */
	public MemoryRegion(int memoryPointer, boolean unfilled, int size) {
		memPointer = memoryPointer;
		maxWritePointer = 0;
		regionBaseAddress = 0;
		this.unfilled = unfilled;
		buffer = ByteBuffer.allocate(size).order(LITTLE_ENDIAN);
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

	/**
	 * Write a chunk of data into the region, updating the maximum point where
	 * the writes occurred.
	 *
	 * @param data
	 *            The chunk of data to write at the current write pointer.
	 */
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

	/**
	 * Set the write pointer. The write pointer is where the next block of data
	 * to be written will actually be written.
	 *
	 * @param address
	 *            The address to set.
	 */
	public void setWritePointer(int address) {
		buffer.position(address);
		maxWritePointer = max(maxWritePointer, address);
	}

	/**
	 * Get the address of the first byte in the region.
	 *
	 * @return The address.
	 */
	public int getRegionBase() {
		return regionBaseAddress;
	}
 	/**
	 * Set the address of the first byte in the region.
	 *
	 * @param address
	 *            The address to set.
	 */
	public void setRegionBase(int address) {
		regionBaseAddress = address;
	}
}
