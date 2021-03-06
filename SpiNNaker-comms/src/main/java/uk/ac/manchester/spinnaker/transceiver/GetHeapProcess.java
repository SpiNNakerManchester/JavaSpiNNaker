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
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.Integer.toUnsignedLong;
import static java.util.Collections.unmodifiableList;
import static uk.ac.manchester.spinnaker.messages.Constants.SYSTEM_VARIABLE_BASE_ADDRESS;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.HeapElement;
import uk.ac.manchester.spinnaker.messages.model.SARKField;
import uk.ac.manchester.spinnaker.messages.model.SARKStruct;
import uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;

/**
 * Get a description of the heap.
 */
class GetHeapProcess extends MultiConnectionProcess<SCPConnection> {
	private static final int HEAP_HEADER_SIZE = 16;

	private static final int HEAP_BLOCK_HEADER_SIZE = 8;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	GetHeapProcess(ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Get the heap block descriptors.
	 *
	 * @param chip
	 *            The chip to ask.
	 * @param heap
	 *            The heap to ask about.
	 * @return A list of block descriptors, in block chain order.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	List<HeapElement> getBlocks(HasChipLocation chip,
			SystemVariableDefinition heap)
			throws IOException, ProcessException {
		HeapHeader header = getHeapHeader(chip, heap);
		long nextBlock = header.first;

		List<HeapElement> blocks = new ArrayList<>();

		while (nextBlock != 0) {
			BlockHeader block = getBlockHeader(chip, nextBlock);
			if (block.next != 0) {
				blocks.add(new HeapElement(nextBlock, block.next, block.free));
			}
			nextBlock = toUnsignedLong(block.next);
		}

		return unmodifiableList(blocks);
	}

	/**
	 * Get the free heap block descriptors.
	 * <p>
	 * <em>WARNING: This is untested code!</em>
	 *
	 * @param chip
	 *            The chip to ask.
	 * @param heap
	 *            The heap to ask about.
	 * @return A list of block descriptors, in block chain order.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	List<HeapElement> getFreeBlocks(HasChipLocation chip,
			SystemVariableDefinition heap)
			throws IOException, ProcessException {
		HeapHeader header = getHeapHeader(chip, heap);
		long nextBlock = header.free;

		List<HeapElement> blocks = new ArrayList<>();

		while (nextBlock != 0 && nextBlock != header.last) {
			BlockHeader block = getBlockHeader(chip, nextBlock);
			if (block.next != 0) {
				blocks.add(new HeapElement(nextBlock, block.next, block.free));
			}
			nextBlock = toUnsignedLong(block.free);
		}

		return unmodifiableList(blocks);
	}

	/**
	 * Get the space free in a heap.
	 *
	 * @param chip
	 *            The chip to ask.
	 * @param heap
	 *            The heap to ask about.
	 * @return The number of bytes free in the heap.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	int getFreeSpace(HasChipLocation chip, SystemVariableDefinition heap)
			throws IOException, ProcessException {
		return getHeapHeader(chip, heap).freeBytes;
	}

	private HeapHeader getHeapHeader(HasChipLocation chip,
			SystemVariableDefinition heap)
			throws IOException, ProcessException {
		int heapBase = readFromAddress(chip,
				SYSTEM_VARIABLE_BASE_ADDRESS + heap.offset, heap.type.value)
						.get();
		HeapHeader header = new HeapHeader(
				readFromAddress(chip, heapBase, HEAP_HEADER_SIZE));
		return header;
	}

	private BlockHeader getBlockHeader(HasChipLocation chip, long address)
			throws IOException, ProcessException {
		return new BlockHeader(
				readFromAddress(chip, address, HEAP_BLOCK_HEADER_SIZE));
	}

	// NB: assumes that size is small
	private IntBuffer readFromAddress(HasChipLocation chip, long address,
			long size) throws IOException, ProcessException {
		return synchronousCall(
				new ReadMemory(chip, (int) address, (int) size)).data
						.asIntBuffer();
	}

	@SARKStruct("heap_t")
	private static class HeapHeader {
		@SARKField("free")
		final long free;

		@SARKField("first")
		final long first;

		@SARKField("last")
		final long last;

		@SARKField("free_bytes")
		final int freeBytes;

		HeapHeader(IntBuffer data) {
			free = toUnsignedLong(data.get());
			first = toUnsignedLong(data.get());
			last = toUnsignedLong(data.get());
			freeBytes = data.get();
			// Note that we don't read or look at the 'buffer' field
		}
	}

	@SARKStruct("block_t")
	private static class BlockHeader {
		@SARKField("next")
		final int next;

		@SARKField("free")
		final int free;

		BlockHeader(IntBuffer data) {
			next = data.get();
			free = data.get();
		}
	}
}
