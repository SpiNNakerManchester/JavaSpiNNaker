package uk.ac.manchester.spinnaker.processes;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.ReadLink;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;

/** A process for reading memory on a SpiNNaker chip. */
public class ReadMemoryProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 */
	public ReadMemoryProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
	}

	private static class Accumulator {
		private final ByteBuffer buffer;
		private boolean done = false;
		private int maxpos = 0;

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
			maxpos = max(maxpos, b.position());
		}

		synchronized ByteBuffer finish() {
			done = true;
			buffer.position(maxpos).flip();
			return buffer.asReadOnlyBuffer().order(LITTLE_ENDIAN);
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

	/**
	 * Read memory over a link into a prepared buffer.
	 *
	 * @param chip
	 *            What chip does the link start at.
	 * @param linkID
	 *            the ID of the link to traverse.
	 * @param baseAddress
	 *            where to read from.
	 * @param receivingBuffer
	 *            The buffer to receive into; the remaining space of the buffer
	 *            determines how much memory to read.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
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

	/**
	 * Read memory into a prepared buffer.
	 *
	 * @param chip
	 *            What chip has the memory to read from.
	 * @param baseAddress
	 *            where to read from.
	 * @param receivingBuffer
	 *            The buffer to receive into; the remaining space of the buffer
	 *            determines how much memory to read.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
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

	/**
	 * Read memory over a link into a new buffer.
	 *
	 * @param chip
	 *            What chip does the link start at.
	 * @param linkID
	 *            the ID of the link to traverse.
	 * @param baseAddress
	 *            where to read from.
	 * @param size
	 *            The number of bytes to read.
	 * @return the filled buffer
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
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

	/**
	 * Read memory into a new buffer.
	 *
	 * @param chip
	 *            What chip has the memory to read from.
	 * @param baseAddress
	 *            where to read from.
	 * @param size
	 *            The number of bytes to read.
	 * @return the filled buffer
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
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
	 *
	 * @param chip
	 *            What chip does the link start at.
	 * @param linkID
	 *            the ID of the link to traverse.
	 * @param baseAddress
	 *            where to read from.
	 * @param size
	 *            The number of bytes to read.
	 * @param dataFile
	 *            where to write the bytes
	 * @throws IOException
	 *             If anything goes wrong with networking or with access to the
	 *             file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
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
	 *
	 * @param chip
	 *            What chip has the memory to read from.
	 * @param baseAddress
	 *            where to read from.
	 * @param size
	 *            The number of bytes to read.
	 * @param dataFile
	 *            where to write the bytes
	 * @throws IOException
	 *             If anything goes wrong with networking or with access to the
	 *             file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
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

	/**
	 * Read memory over a link into a file.
	 *
	 * @param chip
	 *            What chip does the link start at.
	 * @param linkID
	 *            the ID of the link to traverse.
	 * @param baseAddress
	 *            where to read from.
	 * @param size
	 *            The number of bytes to read.
	 * @param dataFile
	 *            where to write the bytes
	 * @throws IOException
	 *             If anything goes wrong with networking or with access to the
	 *             file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public void readLink(HasChipLocation chip, int linkID, int baseAddress,
			int size, File dataFile) throws IOException, Exception {
		try (RandomAccessFile s = new RandomAccessFile(dataFile, "rw")) {
			readLink(chip, linkID, baseAddress, size, s);
		}
	}

	/**
	 * Read memory into a file.
	 *
	 * @param chip
	 *            What chip has the memory to read from.
	 * @param baseAddress
	 *            where to read from.
	 * @param size
	 *            The number of bytes to read.
	 * @param dataFile
	 *            where to write the bytes
	 * @throws IOException
	 *             If anything goes wrong with networking or with access to the
	 *             file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public void readMemory(HasChipLocation chip, int baseAddress, int size,
			File dataFile) throws IOException, Exception {
		try (RandomAccessFile s = new RandomAccessFile(dataFile, "rw")) {
			readMemory(chip, baseAddress, size, s);
		}
	}
}
