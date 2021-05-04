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
package uk.ac.manchester.spinnaker.messages.model;

/** An element of one of the heaps on SpiNNaker. */
@SARKStruct("block")
public class HeapElement {
	/** The address of the block. */
	public final int blockAddress;

	/** A pointer to the next block, or 0 if none. */
	@SARKField("next")
	public final int nextAddress;

	/** The usable size of this block (not including the header). */
	public final int size;

	// Note that multiple fields are encoded in the free field.

	/** True if the block is free. */
	@SARKField("free")
	public final boolean isFree;

	/** The tag of the block if allocated, or {@code null} if not. */
	@SARKField("free")
	public final Integer tag;

	/**
	 * The application ID of the block if allocated, or {@code null} if not.
	 */
	@SARKField("free")
	public final AppID appID;

	private static final int FREE_MASK = 0xFFFF0000;

	private static final int BYTE_MASK = 0x000000FF;

	// WORD := [ BYTE3 | BYTE2 | BYTE1 | BYTE0 ]
	private static final int BYTE1_SHIFT = 8;

	private static final int BLOCK_HEADER_SIZE = 8;

	/**
	 * @param blockAddress
	 *            The address of this element on the heap
	 * @param nextAddress
	 *            The address of the next element on the heap
	 * @param free
	 *            The "free" element of the block as read from the heap
	 */
	public HeapElement(int blockAddress, int nextAddress, int free) {
		this.blockAddress = blockAddress;
		this.nextAddress = nextAddress;
		this.isFree = (free & FREE_MASK) != FREE_MASK;
		if (isFree) {
			tag = null;
			appID = null;
		} else {
			tag = free & BYTE_MASK;
			appID = new AppID((free >>> BYTE1_SHIFT) & BYTE_MASK);
		}
		size = nextAddress - blockAddress - BLOCK_HEADER_SIZE;
	}

	/**
	 * @param blockAddress
	 *            The address of this element on the heap
	 * @param nextAddress
	 *            The address of the next element on the heap
	 * @param free
	 *            The "free" element of the block as read from the heap
	 */
	public HeapElement(long blockAddress, int nextAddress, int free) {
		this.blockAddress = (int) blockAddress;
		this.nextAddress = nextAddress;
		this.isFree = (free & FREE_MASK) != FREE_MASK;
		if (isFree) {
			tag = null;
			appID = null;
		} else {
			tag = free & BYTE_MASK;
			appID = new AppID((free >>> BYTE1_SHIFT) & BYTE_MASK);
		}
		size = (int) (nextAddress - blockAddress) - BLOCK_HEADER_SIZE;
	}

	/**
	 * Gets the address of the data in the heap element.
	 *
	 * @return The address of the data ({@link #size} bytes long) that
	 *         immediately follows the heap element header.
	 */
	public final int getDataAddress() {
		return blockAddress + BLOCK_HEADER_SIZE;
	}
}
