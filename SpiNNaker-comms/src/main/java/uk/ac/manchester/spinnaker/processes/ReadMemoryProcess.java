package uk.ac.manchester.spinnaker.processes;

import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.ReadLink;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.selectors.ConnectionSelector;

/** A process for reading memory on a SpiNNaker chip. */
public class ReadMemoryProcess extends MultiConnectionProcess {
	public ReadMemoryProcess(ConnectionSelector connectionSelector) {
		super(connectionSelector);
	}

	private static class Accumulator {
		private final ByteBuffer buffer;
		private boolean done = false;

		Accumulator(int size) {
			buffer = allocate(size).order(LITTLE_ENDIAN);
		}

		Accumulator(ByteBuffer receivingBuffer) {
			buffer = receivingBuffer.slice();
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

		synchronized ByteBuffer finish() {
			done = true;
			buffer.flip();
			return buffer.asReadOnlyBuffer();
		}
	}

	/**
	 * This is complicated because the writes to the file can happen out of
	 * order if the other end serves up the responses out of order. To handle
	 * this, we need a seekable stream, and we need to make sure that we're not
	 * stomping on our own toes when we do the seek.
	 */
	private static class FileAccumulator {
		private final RandomAccessFile file;
		private final long initOffset;
		private boolean done = false;
		private IOException exception;

		FileAccumulator(RandomAccessFile dataStream) throws IOException {
			file = dataStream;
			initOffset = dataStream.getFilePointer();
		}

		synchronized void add(int position, ByteBuffer buffer) {
			if (done) {
				throw new IllegalStateException(
						"writing to fully written buffer");
			}
			try {
				file.seek(position + initOffset);
				file.write(buffer.array(), buffer.position(),
						buffer.remaining());
			} catch (IOException e) {
				if (exception == null) {
					exception = e;
				}
			}
		}

		synchronized void finish() throws IOException {
			done = true;
			if (exception != null) {
				throw exception;
			}
			file.seek(initOffset);
		}
	}

	/** Read memory over a link into a prepared buffer. */
	public void readLink(HasChipLocation chip, int linkID, int baseAddress,
			ByteBuffer receivingBuffer) throws IOException, Exception {
		int size = receivingBuffer.remaining();
		Accumulator a = new Accumulator(receivingBuffer);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size, UDP_MESSAGE_MAX_SIZE);
			int thisOffset = offset;
			sendRequest(new ReadLink(chip, linkID, baseAddress + offset, chunk),
					response -> a.add(thisOffset, response.data));
		}
		finish();
		checkForError();
		a.finish();
	}

	/** Read memory into a prepared buffer. */
	public void readMemory(HasChipLocation chip, int baseAddress,
			ByteBuffer receivingBuffer) throws IOException, Exception {
		int size = receivingBuffer.remaining();
		Accumulator a = new Accumulator(receivingBuffer);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size, UDP_MESSAGE_MAX_SIZE);
			int thisOffset = offset;
			sendRequest(new ReadMemory(chip, baseAddress + offset, chunk),
					response -> a.add(thisOffset, response.data));
		}
		finish();
		checkForError();
		a.finish();
	}

	/** Read memory over a link into a new buffer. */
	public ByteBuffer readLink(HasChipLocation chip, int linkID,
			int baseAddress, int size) throws IOException, Exception {
		Accumulator a = new Accumulator(size);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size, UDP_MESSAGE_MAX_SIZE);
			int thisOffset = offset;
			sendRequest(new ReadLink(chip, linkID, baseAddress + offset, chunk),
					response -> a.add(thisOffset, response.data));
		}
		finish();
		checkForError();
		return a.finish();
	}

	/** Read memory into a new buffer. */
	public ByteBuffer readMemory(HasChipLocation chip, int baseAddress,
			int size) throws IOException, Exception {
		Accumulator a = new Accumulator(size);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size, UDP_MESSAGE_MAX_SIZE);
			int thisOffset = offset;
			sendRequest(new ReadMemory(chip, baseAddress + offset, chunk),
					response -> a.add(thisOffset, response.data));
		}
		finish();
		checkForError();
		return a.finish();
	}

	/**
	 * Read memory over a link into a file. Note that we can write the file out
	 * of order; a {@link RandomAccessFile} is required
	 */
	public void readLink(HasChipLocation chip, int linkID, int baseAddress,
			int size, RandomAccessFile dataFile) throws IOException, Exception {
		FileAccumulator a = new FileAccumulator(dataFile);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size, UDP_MESSAGE_MAX_SIZE);
			int thisOffset = offset;
			sendRequest(new ReadLink(chip, linkID, baseAddress + offset, chunk),
					response -> a.add(thisOffset, response.data));
		}
		finish();
		checkForError();
		a.finish();
	}

	/**
	 * Read memory into a file. Note that we can write the file out of order; a
	 * {@link RandomAccessFile} is required
	 */
	public void readMemory(HasChipLocation chip, int baseAddress, int size,
			RandomAccessFile dataFile) throws IOException, Exception {
		FileAccumulator a = new FileAccumulator(dataFile);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size, UDP_MESSAGE_MAX_SIZE);
			int thisOffset = offset;
			sendRequest(new ReadMemory(chip, baseAddress + offset, chunk),
					response -> a.add(thisOffset, response.data));
		}
		finish();
		checkForError();
		a.finish();
	}

	/** Read memory over a link into a file. */
	public void readLink(HasChipLocation chip, int linkID, int baseAddress,
			int size, File dataFile) throws IOException, Exception {
		try (RandomAccessFile s = new RandomAccessFile(dataFile, "rw")) {
			readLink(chip, linkID, baseAddress, size, s);
		}
	}

	/** Read memory into a file. */
	public void readMemory(HasChipLocation chip, int baseAddress, int size,
			File dataFile) throws IOException, Exception {
		try (RandomAccessFile s = new RandomAccessFile(dataFile, "rw")) {
			readMemory(chip, baseAddress, size, s);
		}
	}
}
