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

import static java.util.Collections.unmodifiableList;
import static uk.ac.manchester.spinnaker.transceiver.CommonMemoryLocations.SYS_VARS;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.model.HeapElement;
import uk.ac.manchester.spinnaker.messages.model.SARKField;
import uk.ac.manchester.spinnaker.messages.model.SARKStruct;
import uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;

/**
 * Get a description of the heap.
 */
final class GetHeapProcess extends TxrxProcess {
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
	GetHeapProcess(
			ConnectionSelector<? extends SCPConnection> connectionSelector,
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	List<HeapElement> getBlocks(HasChipLocation chip,
			SystemVariableDefinition heap)
			throws IOException, ProcessException, InterruptedException {
		var header = getHeapHeader(chip, heap);
		var nextBlock = header.first;

		var blocks = new ArrayList<HeapElement>();

		while (!nextBlock.isNull()) {
			var block = getBlockHeader(chip, nextBlock);
			if (!block.next.isNull()) {
				blocks.add(new HeapElement(nextBlock, block.next,
						block.free.address()));
			}
			nextBlock = block.next;
		}

		return unmodifiableList(blocks);
	}

	/**
	 * Get the free heap block descriptors.
	 * <p>
	 * <em><strong>WARNING:</strong> This is untested code!</em>
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	List<HeapElement> getFreeBlocks(HasChipLocation chip,
			SystemVariableDefinition heap)
			throws IOException, ProcessException, InterruptedException {
		var header = getHeapHeader(chip, heap);
		var nextBlock = header.free;

		var blocks = new ArrayList<HeapElement>();

		while (!nextBlock.isNull() && !nextBlock.equals(header.last)) {
			var block = getBlockHeader(chip, nextBlock);
			if (!block.next.isNull()) {
				blocks.add(new HeapElement(nextBlock, block.next,
						block.free.address()));
			}
			nextBlock = block.free;
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	int getFreeSpace(HasChipLocation chip, SystemVariableDefinition heap)
			throws IOException, ProcessException, InterruptedException {
		return getHeapHeader(chip, heap).freeBytes;
	}

	/**
	 * Read a heap header.
	 *
	 * @param chip
	 *            What chip to read from.
	 * @param heap
	 *            Which heap to get the header of.
	 * @return The heap header.
	 * @throws IOException
	 *             If anything goes wrong with networking
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	private HeapHeader getHeapHeader(HasChipLocation chip,
			SystemVariableDefinition heap)
			throws IOException, ProcessException, InterruptedException {
		var heapBase = new MemoryLocation(readFromAddress(chip,
				SYS_VARS.add(heap.offset), heap.type.value).get());
		return new HeapHeader(
				readFromAddress(chip, heapBase, HEAP_HEADER_SIZE));
	}

	/**
	 * Read a memory block header.
	 *
	 * @param chip
	 *            What chip to read from.
	 * @param address
	 *            What address to read from.
	 * @return The memory block header.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	private BlockHeader getBlockHeader(HasChipLocation chip,
			MemoryLocation address)
			throws IOException, ProcessException, InterruptedException {
		return new BlockHeader(
				readFromAddress(chip, address, HEAP_BLOCK_HEADER_SIZE));
	}

	/**
	 * Simplified read. <em>Assumes</em> that the amount of data being read can
	 * fit in a single response message.
	 *
	 * @param chip
	 *            What chip to read from.
	 * @param address
	 *            What address to read from.
	 * @param size
	 *            How much to read.
	 * @return Data read, wrapped as little-endian integer buffer.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	private IntBuffer readFromAddress(HasChipLocation chip,
			MemoryLocation address, long size)
			throws IOException, ProcessException, InterruptedException {
		return retrieve(new ReadMemory(chip, address, (int) size))
				.asIntBuffer();
	}

	@SARKStruct("heap_t")
	private record HeapHeader(//
			@SARKField("free") MemoryLocation free,
			@SARKField("first") MemoryLocation first,
			@SARKField("last") MemoryLocation last,
			@SARKField("free_bytes") int freeBytes) {
		HeapHeader(IntBuffer data) {
			this(//
					new MemoryLocation(data.get()),
					new MemoryLocation(data.get()),
					new MemoryLocation(data.get()), //
					data.get());
			// Note that we don't read or look at the 'buffer' field
		}
	}

	@SARKStruct("block_t")
	private record BlockHeader(//
			@SARKField("next") MemoryLocation next,
			@SARKField("free") MemoryLocation free) {
		BlockHeader(IntBuffer data) {
			this(//
					new MemoryLocation(data.get()),
					new MemoryLocation(data.get()));
		}
	}
}
