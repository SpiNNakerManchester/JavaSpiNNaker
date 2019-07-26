/*
 * Copyright (c) 2018-2019 The University of Manchester
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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.ReadLink;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;

/** A process for reading memory on a SpiNNaker chip. */
class ReadMemoryProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	ReadMemoryProcess(ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	private static class Accumulator {
		private final ByteBuffer buffer;
		private boolean done = false;
		private int maxpos = 0;

		Accumulator(int size) {
			buffer = allocate(size);
		}

		Accumulator(byte[] receivingBuffer) {
			buffer = wrap(receivingBuffer);
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

	/**
	 * Read memory over a link into a prepared buffer.
	 *
	 * @param chip
	 *            What chip does the link start at.
	 * @param linkDirection
	 *            The direction of the link to traverse.
	 * @param baseAddress
	 *            Where to read from.
	 * @param receivingBuffer
	 *            The buffer to receive into; the remaining space of the buffer
	 *            determines how much memory to read.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public void readLink(HasChipLocation chip, Direction linkDirection,
			int baseAddress, ByteBuffer receivingBuffer)
			throws IOException, ProcessException {
		int size = receivingBuffer.remaining();
		Accumulator a = new Accumulator(receivingBuffer);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size - offset, UDP_MESSAGE_MAX_SIZE);
			int thisOffset = offset;
			sendRequest(
					new ReadLink(chip, linkDirection, baseAddress + offset,
							chunk),
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
	 * @param linkDirection
	 *            the direction of the link to traverse.
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
	public ByteBuffer readLink(HasChipLocation chip, Direction linkDirection,
			int baseAddress, int size) throws IOException, ProcessException {
		Accumulator a = new Accumulator(size);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size - offset, UDP_MESSAGE_MAX_SIZE);
			int thisOffset = offset;
			sendRequest(
					new ReadLink(chip, linkDirection, baseAddress + offset,
							chunk),
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
	 * @param linkDirection
	 *            The direction of the link to traverse.
	 * @param baseAddress
	 *            Where to read from.
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
	public void readLink(HasChipLocation chip, Direction linkDirection,
			int baseAddress, int size, RandomAccessFile dataFile)
			throws IOException, ProcessException {
		FileAccumulator a = new FileAccumulator(dataFile);
		int chunk;
		for (int offset = 0; offset < size; offset += chunk) {
			chunk = min(size - offset, UDP_MESSAGE_MAX_SIZE);
			int thisOffset = offset;
			sendRequest(
					new ReadLink(chip, linkDirection, baseAddress + offset,
							chunk),
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
	 *            Where to read from.
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
	 * @param linkDirection
	 *            The direction of the link to traverse.
	 * @param baseAddress
	 *            Where to read from.
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
	public void readLink(HasChipLocation chip, Direction linkDirection,
			int baseAddress, int size, File dataFile)
			throws IOException, ProcessException {
		try (RandomAccessFile s = new RandomAccessFile(dataFile, "rw")) {
			readLink(chip, linkDirection, baseAddress, size, s);
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
	 * Read memory into a database from a recording region.
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
	public void readMemory(BufferManagerStorage.Region region,
			BufferManagerStorage storage)
			throws IOException, ProcessException, StorageException {
		byte[] buffer = new byte[region.size];
		Accumulator a = new Accumulator(buffer);
		int chunk;
		for (int offset = 0; offset < region.size; offset += chunk) {
			chunk = min(region.size - offset, UDP_MESSAGE_MAX_SIZE);
			int thisOffset = offset;
			sendRequest(
					new ReadMemory(region.core.asChipLocation(),
							region.startAddress + offset, chunk),
					response -> a.add(thisOffset, response.data));
		}
		finish();
		checkForError();
		a.finish();
		storage.appendRecordingContents(region, buffer);
	}
}
