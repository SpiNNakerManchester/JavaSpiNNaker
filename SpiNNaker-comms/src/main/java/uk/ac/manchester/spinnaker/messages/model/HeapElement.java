/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.model;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/** An element of one of the heaps on SpiNNaker. */
@SARKStruct("block")
public class HeapElement {
	/** The address of the block. */
	public final MemoryLocation blockAddress;

	/** A pointer to the next block, or 0 if none. */
	@SARKField("next")
	public final MemoryLocation nextAddress;

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
	public HeapElement(MemoryLocation blockAddress, MemoryLocation nextAddress,
			int free) {
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
		size = nextAddress.diff(blockAddress) - BLOCK_HEADER_SIZE;
	}

	/**
	 * Gets the address of the data in the heap element.
	 *
	 * @return The address of the data ({@link #size} bytes long) that
	 *         immediately follows the heap element header.
	 */
	public final MemoryLocation getDataAddress() {
		return blockAddress.add(BLOCK_HEADER_SIZE);
	}
}
