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

import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/**
 * An element of one of the heaps on SpiNNaker.
 *
 * @param blockAddress
 *            The address of the block.
 * @param nextAddress
 *            A pointer to the next block, or {@link MemoryLocation#NULL} if
 *            none.
 * @param size
 *            The usable size of this block (not including the header).
 * @param isFree
 *            True if the block is free.
 * @param tag
 *            The tag of the block if allocated, or {@code null} if not.
 * @param appID
 *            The application ID of the block if allocated, or {@code null} if
 *            not.
 */
@SARKStruct("block")
public record HeapElement(MemoryLocation blockAddress,
		@SARKField("next") MemoryLocation nextAddress, int size,
		// Note that multiple fields are encoded in the free field.
		@SARKField("free") boolean isFree, @SARKField("free") Integer tag,
		@SARKField("free") AppID appID) {
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
	public HeapElement(MemoryLocation blockAddress, MemoryLocation nextAddress,
			int free) {
		// Chain via another constructor so we have convenient access to isFree
		this(blockAddress, nextAddress, free, (free & FREE_MASK) != FREE_MASK);
	}

	private HeapElement(MemoryLocation blockAddress, MemoryLocation nextAddress,
			int free, boolean isFree) {
		this(blockAddress, nextAddress,
				nextAddress.diff(blockAddress) - BLOCK_HEADER_SIZE, isFree,
				isFree ? null : free & BYTE_MASK,
				isFree ? null : new AppID((free >>> BYTE1_SHIFT) & BYTE_MASK));
	}

	/**
	 * Gets the address of the data in the heap element.
	 *
	 * @return The address of the data ({@link #size} bytes long) that
	 *         immediately follows the heap element header.
	 */
	public final MemoryLocation dataAddress() {
		return blockAddress.add(BLOCK_HEADER_SIZE);
	}
}
