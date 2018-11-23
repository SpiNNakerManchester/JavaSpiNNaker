package uk.ac.manchester.spinnaker.transceiver.processes;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.ReadLink;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.storage.Storage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/** A process for reading memory on a SpiNNaker chip. */
public class ReadMemoryProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public ReadMemoryProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * How much data do we want to hit the database with in one go? This is
	 * applied only for database writes because the database is more efficiently
	 * written that way; when going to a memory buffer or random access file,
	 * there's not really any point.
	 */
	private static final int DATABASE_WAIT_CHUNK = 0x20000;

	private static class Accumulator {
		private final ByteBuffer buffer;
		private boolean done = false;
		private int maxpos = 0;

		Accumulator(int size) {
			buffer = allocate(size);
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
			int after = position + otherBuffer.remaining();
			b.put(otherBuffer);
			maxpos = max(maxpos, after);
		}

		synchronized ByteBuffer finish() {
			if (!done) {
				done = true;
				buffer.limit(maxpos);
			}
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

	private static class DBAccumulator {
		private final Storage storage;
		private final Storage.Region region;
		private final Integer recordingIndex;
		private final Map<Integer, ByteBuffer> writes;
		private boolean done = false;
		private StorageException exception;

		DBAccumulator(Storage storage, Storage.Region region,
				Integer recordingIndex) {
			this.storage = storage;
			this.region = region;
			this.recordingIndex = recordingIndex;
			this.writes = new LinkedHashMap<>();
		}

		synchronized int bookSlot(int offset) {
			if (done) {
				throw new IllegalStateException(
						"writing to fully written buffer");
			}
			writes.put(offset, null);
			return offset;
		}

		synchronized void add(int offset, ByteBuffer data) {
			if (done) {
				throw new IllegalStateException(
						"writing to fully written buffer");
			}
			writes.put(offset, data);
			Iterator<Entry<Integer, ByteBuffer>> entries =
					writes.entrySet().iterator();
			while (entries.hasNext()) {
				Entry<Integer, ByteBuffer> ent = entries.next();
				if (ent.getValue() == null) {
					break;
				}
				try {
					store(ent.getValue());
				} catch (StorageException e) {
					if (exception == null) {
						exception = e;
					}
				}
				entries.remove();
			}
			return;
		}

		private void store(ByteBuffer buffer) throws StorageException {
			if (recordingIndex == null) {
				storage.storeRegionContents(region, buffer);
			} else {
				storage.appendRecordingContents(region, recordingIndex, buffer);
			}
		}

		synchronized void finish() throws StorageException {
			done = true;
			if (exception != null) {
				throw exception;
			}
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public void readLink(HasChipLocation chip, int linkID, int baseAddress,
			ByteBuffer receivingBuffer) throws IOException, ProcessException {
		int size = receivingBuffer.remaining();
		Accumulator a = new Accumulator(receivingBuffer);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size - offset, UDP_MESSAGE_MAX_SIZE);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public void readMemory(HasChipLocation chip, int baseAddress,
			ByteBuffer receivingBuffer) throws IOException, ProcessException {
		int size = receivingBuffer.remaining();
		Accumulator a = new Accumulator(receivingBuffer);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size - offset, UDP_MESSAGE_MAX_SIZE);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public ByteBuffer readLink(HasChipLocation chip, int linkID,
			int baseAddress, int size) throws IOException, ProcessException {
		Accumulator a = new Accumulator(size);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size - offset, UDP_MESSAGE_MAX_SIZE);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public ByteBuffer readMemory(HasChipLocation chip, int baseAddress,
			int size) throws IOException, ProcessException {
		Accumulator a = new Accumulator(size);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size - offset, UDP_MESSAGE_MAX_SIZE);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public void readLink(HasChipLocation chip, int linkID, int baseAddress,
			int size, RandomAccessFile dataFile)
			throws IOException, ProcessException {
		FileAccumulator a = new FileAccumulator(dataFile);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size - offset, UDP_MESSAGE_MAX_SIZE);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public void readMemory(HasChipLocation chip, int baseAddress, int size,
			RandomAccessFile dataFile) throws IOException, ProcessException {
		FileAccumulator a = new FileAccumulator(dataFile);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size - offset, UDP_MESSAGE_MAX_SIZE);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public void readLink(HasChipLocation chip, int linkID, int baseAddress,
			int size, File dataFile) throws IOException, ProcessException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public void readMemory(HasChipLocation chip, int baseAddress, int size,
			File dataFile) throws IOException, ProcessException {
		try (RandomAccessFile s = new RandomAccessFile(dataFile, "rw")) {
			readMemory(chip, baseAddress, size, s);
		}
	}

	/**
	 * Read memory into a database from a DSE-allocated region.
	 *
	 * @param region
	 *            What region of the chip is being read. This is used to
	 *            organise the data within the database as well as to specify
	 *            where to read.
	 * @param storage
	 *            where to write the bytes
	 * @throws IOException
	 *             If anything goes wrong with networking or with access to the
	 *             file.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If anything goes wrong with access to the database.
	 */
	public void readMemory(Storage.Region region, Storage storage)
			throws IOException, ProcessException, StorageException {
		DBAccumulator a = new DBAccumulator(storage, region, null);
		int chunk;
		for (int offset = 0, finishPoint = 0; offset < region.size; offset +=
				chunk) {
			chunk = min(region.size - offset, UDP_MESSAGE_MAX_SIZE);
			int thisOffset = a.bookSlot(offset);
			sendRequest(
					new ReadMemory(region.core.asChipLocation(),
							region.startAddress + offset, chunk),
					response -> a.add(thisOffset, response.data));
			// Apply wait chunking
			if (thisOffset > finishPoint + DATABASE_WAIT_CHUNK) {
				finish();
				finishPoint = thisOffset;
			}
		}
		finish();
		checkForError();
		a.finish();
	}

	/**
	 * Read memory into a database from a recording region.
	 *
	 * @param region
	 *            What region of the chip is being read. This is used to
	 *            organise the data within the database as well as to specify
	 *            where to read.
	 * @param recordingIndex
	 *            What recording region (associated with the main region) is
	 *            being pulled.
	 * @param storage
	 *            where to write the bytes
	 * @throws IOException
	 *             If anything goes wrong with networking or with access to the
	 *             file.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If anything goes wrong with access to the database.
	 */
	public void readMemory(Storage.Region region, int recordingIndex,
			Storage storage)
			throws IOException, ProcessException, StorageException {
		DBAccumulator a = new DBAccumulator(storage, region, recordingIndex);
		int chunk;
		for (int offset = 0, finishPoint = 0; offset < region.size; offset +=
				chunk) {
			chunk = min(region.size - offset, UDP_MESSAGE_MAX_SIZE);
			int thisOffset = a.bookSlot(offset);
			sendRequest(
					new ReadMemory(region.core.asChipLocation(),
							region.startAddress + offset, chunk),
					response -> a.add(thisOffset, response.data));
			// Apply wait chunking
			if (thisOffset > finishPoint + DATABASE_WAIT_CHUNK) {
				finish();
				finishPoint = thisOffset;
			}
		}
		finish();
		checkForError();
		a.finish();
	}
}
