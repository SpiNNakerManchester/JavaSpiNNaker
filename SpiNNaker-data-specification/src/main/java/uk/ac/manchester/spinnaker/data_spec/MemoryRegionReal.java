/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.data_spec;

import static java.lang.Math.max;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;

/**
 * A memory region storage object.
 * <p>
 * As memory regions are modifiable objects, their equality is defined in terms
 * of their fundamental object identity.
 */
public final class MemoryRegionReal extends MemoryRegion {

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

	/** The index of the memory region. */
	private final int index;

	/** A reference for the region, {@code null} if none. */
	private final Integer reference;

	/**
	 * Create a memory region.
	 *
	 * @param index
	 *            the index of the memory region
	 * @param memoryPointer
	 *            where the start of the block is
	 * @param unfilled
	 *            whether this is an unfilled region
	 * @param size
	 *            the allocated size of the memory region
	 */
	MemoryRegionReal(int index, int memoryPointer, boolean unfilled, int size) {
		this.index = index;
		memPointer = memoryPointer;
		maxWritePointer = 0;
		regionBaseAddress = 0;
		this.unfilled = unfilled;
		reference = null;
		buffer = allocate(size).order(LITTLE_ENDIAN);
	}

	/**
	 * Create a memory region with a reference.
	 *
	 * @param index
	 *            the index of the memory region
	 * @param memoryPointer
	 *            where the start of the block is
	 * @param unfilled
	 *            whether this is an unfilled region
	 * @param size
	 *            the allocated size of the memory region
	 * @param reference
	 *            the reference of the memory region
	 */
	MemoryRegionReal(int index, int memoryPointer, boolean unfilled, int size,
			int reference) {
		this.index = index;
		memPointer = memoryPointer;
		maxWritePointer = 0;
		regionBaseAddress = 0;
		this.unfilled = unfilled;
		this.reference = reference;
		buffer = allocate(size).order(LITTLE_ENDIAN);
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

	@Override
	public int getIndex() {
		return index;
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

	@Override
	public int getRegionBase() {
		return regionBaseAddress;
	}

	@Override
	protected void setRegionBase(int address) {
		regionBaseAddress = address;
	}

	/**
	 * Get the reference of the region.
	 *
	 * @return The reference.
	 * @throws NullPointerException
	 *             if there is no reference.
	 */
	public int getReference() {
		return requireNonNull(reference, "no such reference").intValue();
	}
}
