package uk.ac.manchester.spinnaker.processes;

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

public class GetHeapProcess extends MultiConnectionProcess<SCPConnection> {
	private static final int HEAP_HEADER_SIZE = 8;
	private static final int HEAP_BLOCK_HEADER_SIZE = 8;

	public GetHeapProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
	}

	public List<HeapElement> getBlocks(HasChipLocation chip,
			SystemVariableDefinition heap) throws IOException, Exception {
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
			int size) throws IOException, Exception {
		return synchronousCall(new ReadMemory(chip, address, size)).data
				.asIntBuffer();
	}
}
