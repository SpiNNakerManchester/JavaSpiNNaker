package uk.ac.manchester.spinnaker.processes;

import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.ReadLink;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;

/** A process for reading memory on a SpiNNaker chip. */
public class ReadMemoryProcess extends MultiConnectionProcess {
	public ReadMemoryProcess(ConnectionSelector connectionSelector) {
		super(connectionSelector);
	}

	static class Accumulator {
		private final ByteBuffer buffer;
		private boolean done = false;

		Accumulator(int size) {
			buffer = allocate(size).order(LITTLE_ENDIAN);
		}

		synchronized void add(int position, ByteBuffer otherBuffer) {
			if (done) {
				throw new IllegalStateException(
						"writing to fully written buffer");
			}
			ByteBuffer b = buffer.duplicate();
			b.position(position);
			b.put(otherBuffer);
		}

		synchronized ByteBuffer get() {
			done = true;
			buffer.flip();
			return buffer.asReadOnlyBuffer();
		}
	}

	public ByteBuffer readLink(HasChipLocation chip, int linkID,
			int baseAddress, int size) throws IOException, Exception {
		Accumulator a = new Accumulator(size);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size, UDP_MESSAGE_MAX_SIZE);
			int bufferPosition = offset;
			sendRequest(new ReadLink(chip, linkID, baseAddress + offset, chunk),
					response -> a.add(bufferPosition, response.data));
		}
		finish();
		checkForError();
		return a.get();
	}

	public ByteBuffer readMemory(HasChipLocation chip, int baseAddress,
			int size) throws IOException, Exception {
		Accumulator a = new Accumulator(size);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size, UDP_MESSAGE_MAX_SIZE);
			int bufferPosition = offset;
			sendRequest(new ReadMemory(chip, baseAddress + offset, chunk),
					response -> a.add(bufferPosition, response.data));
		}
		finish();
		checkForError();
		return a.get();
	}
}
