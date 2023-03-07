/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	/** The maximum point where writes have happened up to within the region. */
	private int maxWritePointer;

	/** The buffer storing the written data. */
	private ByteBuffer buffer;

	/**
	 * Whether this is an unfilled region. Unfilled regions can be written
	 * efficiently as a block of zeroes.
	 */
	private boolean unfilled;

	/** The index of the memory region. */
	private final int index;

	/** A reference for the region, {@code null} if none. */
	private final Reference reference;

	/**
	 * Create a memory region.
	 *
	 * @param index
	 *            the index of the memory region
	 * @param unfilled
	 *            whether this is an unfilled region
	 * @param size
	 *            the allocated size of the memory region
	 */
	MemoryRegionReal(int index, boolean unfilled, int size) {
		this.index = index;
		maxWritePointer = 0;
		this.unfilled = unfilled;
		reference = null;
		buffer = allocate(size).order(LITTLE_ENDIAN);
	}

	/**
	 * Create a memory region with a reference.
	 *
	 * @param index
	 *            the index of the memory region
	 * @param unfilled
	 *            whether this is an unfilled region
	 * @param size
	 *            the allocated size of the memory region
	 * @param reference
	 *            the reference of the memory region
	 */
	MemoryRegionReal(int index, boolean unfilled, int size,
			Reference reference) {
		this.index = index;
		maxWritePointer = 0;
		this.unfilled = unfilled;
		this.reference = requireNonNull(reference);
		buffer = allocate(size).order(LITTLE_ENDIAN);
	}

	/** @return The size of the working buffer. */
	public int getAllocatedSize() {
		return buffer.capacity();
	}

	/** @return How much space remains in the region's working buffer. */
	public int getRemainingSpace() {
		return buffer.remaining();
	}

	/** @return Whether this is an unfilled region. */
	public boolean isUnfilled() {
		return unfilled;
	}

	/**
	 * @return The region data. Note that this is a live buffer; its current
	 *         position may be anywhere.
	 */
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

	/** @return the current write pointer */
	public int getWritePointer() {
		return buffer.position();
	}

	/** @return the maximum write pointer */
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

	/**
	 * Get the reference of the region.
	 *
	 * @return The reference.
	 * @throws NullPointerException
	 *             if there is no reference.
	 */
	public Reference getReference() {
		return requireNonNull(reference, "no such reference");
	}
}
