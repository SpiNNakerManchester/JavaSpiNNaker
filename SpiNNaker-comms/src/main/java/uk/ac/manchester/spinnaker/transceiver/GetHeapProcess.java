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
						block.free.address));
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
						block.free.address));
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
	private static class HeapHeader {
		@SARKField("free")
		final MemoryLocation free;

		@SARKField("first")
		final MemoryLocation first;

		@SARKField("last")
		final MemoryLocation last;

		@SARKField("free_bytes")
		final int freeBytes;

		HeapHeader(IntBuffer data) {
			free = new MemoryLocation(data.get());
			first = new MemoryLocation(data.get());
			last = new MemoryLocation(data.get());
			freeBytes = data.get();
			// Note that we don't read or look at the 'buffer' field
		}
	}

	@SARKStruct("block_t")
	private static class BlockHeader {
		@SARKField("next")
		final MemoryLocation next;

		@SARKField("free")
		final MemoryLocation free;

		BlockHeader(IntBuffer data) {
			next = new MemoryLocation(data.get());
			free = new MemoryLocation(data.get());
		}
	}
}
