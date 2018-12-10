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
package uk.ac.manchester.spinnaker.transceiver.processes;

import static java.util.Collections.unmodifiableList;
import static uk.ac.manchester.spinnaker.messages.Constants.SYSTEM_VARIABLE_BASE_ADDRESS;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.HeapElement;
import uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/**
 * Get a description of the heap.
 */
public class GetHeapProcess extends MultiConnectionProcess<SCPConnection> {
	private static final int HEAP_HEADER_SIZE = 8;

	private static final int HEAP_BLOCK_HEADER_SIZE = 8;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public GetHeapProcess(ConnectionSelector<SCPConnection> connectionSelector,
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
	public List<HeapElement> getBlocks(HasChipLocation chip,
			SystemVariableDefinition heap)
			throws IOException, ProcessException {
		int heapBase = readFromAddress(chip,
				SYSTEM_VARIABLE_BASE_ADDRESS + heap.offset, heap.type.value)
						.get();

		IntBuffer data;
		data = readFromAddress(chip, heapBase, HEAP_HEADER_SIZE);
		data.get(); // Advance over one word
		int nextBlock = data.get();

		List<HeapElement> blocks = new ArrayList<>();

		while (nextBlock != 0) {
			data = readFromAddress(chip, nextBlock, HEAP_BLOCK_HEADER_SIZE);
			int next = data.get();
			int free = data.get();
			if (next != 0) {
				blocks.add(new HeapElement(nextBlock, next, free));
			}
			nextBlock = next;
		}

		return unmodifiableList(blocks);
	}

	private IntBuffer readFromAddress(HasChipLocation chip, int address,
			int size) throws IOException, ProcessException {
		return synchronousCall(new ReadMemory(chip, address, size)).data
				.asIntBuffer();
	}
}
