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
