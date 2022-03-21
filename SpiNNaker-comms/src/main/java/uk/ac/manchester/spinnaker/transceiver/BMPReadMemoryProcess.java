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
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.messages.bmp.BMPBoard;
import uk.ac.manchester.spinnaker.messages.bmp.BMPReadMemory;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest.BMPResponse;

/** A process for reading memory on a BMP. */
class BMPReadMemoryProcess extends BMPCommandProcess<BMPResponse> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	BMPReadMemoryProcess(ConnectionSelector<BMPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	interface Accumulator<T> {
		void add(int position, ByteBuffer otherBuffer);

		T finish() throws IOException;
	}

	private static class BufferAccumulator implements Accumulator<ByteBuffer> {
		private final ByteBuffer buffer;

		private boolean done = false;

		private int maxpos = 0;

		BufferAccumulator(int size) {
			buffer = allocate(size);
		}

		BufferAccumulator(ByteBuffer receivingBuffer) {
			buffer = receivingBuffer.slice();
		}

		@Override
		public void add(int position, ByteBuffer otherBuffer) {
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

		@Override
		public ByteBuffer finish() {
			if (!done) {
				done = true;
				buffer.limit(maxpos);
			}
			return buffer.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		}
	}

	private static class FileAccumulator implements Accumulator<Void> {
		private final RandomAccessFile file;

		private final long initOffset;

		private boolean done = false;

		private IOException exception;

		private long pos;

		FileAccumulator(RandomAccessFile dataStream) throws IOException {
			file = dataStream;
			initOffset = dataStream.getFilePointer();
			pos = initOffset;
		}

		@Override
		public void add(int position, ByteBuffer buffer) {
			if (done) {
				throw new IllegalStateException(
						"writing to fully written buffer");
			}
			try {
				long newPos = position + initOffset;
				if (newPos != pos) {
					file.seek(newPos);
					pos = newPos;
				}
				pos += buffer.remaining();
				file.write(buffer.array(), buffer.position(),
						buffer.remaining());
			} catch (IOException e) {
				if (exception == null) {
					exception = e;
				}
			}
		}

		@Override
		public Void finish() throws IOException {
			done = true;
			if (exception != null) {
				throw exception;
			}
			file.seek(initOffset);
			return null;
		}
	}

	/**
	 * Core of the memory read system.
	 *
	 * @param <T>
	 *            The type of output from the accumulator
	 * @param board
	 *            What board's BMP?
	 * @param address
	 *            Where to read from?
	 * @param size
	 *            How much to read?
	 * @param accum
	 *            Where to accumulate the data
	 * @return The accumulation (if defined for the accumulator type).
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	private <T> T read(BMPBoard board, int address, int size,
			Accumulator<T> accum) throws ProcessException, IOException {
		for (int offset = 0, chunk; offset < size; offset += chunk) {
			chunk = min(size - offset, UDP_MESSAGE_MAX_SIZE);
			accum.add(offset, execute(new BMPReadMemory(board, address + offset,
					chunk)).data);
		}
		return accum.finish();
	}

	/**
	 * Read memory into a prepared buffer.
	 *
	 * @param board
	 *            What board's BMP to read from.
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
	void readMemory(BMPBoard board, int baseAddress, ByteBuffer receivingBuffer)
			throws IOException, ProcessException {
		read(board, baseAddress, receivingBuffer.remaining(),
				new BufferAccumulator(receivingBuffer));
		// Ignore the result; caller already knows it
	}

	/**
	 * Read memory into a new buffer.
	 *
	 * @param board
	 *            What board's BMP to read from.
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
	ByteBuffer readMemory(BMPBoard board, int baseAddress, int size)
			throws IOException, ProcessException {
		return read(board, baseAddress, size, new BufferAccumulator(size));
	}

	/**
	 * Read memory into a file. Note that we can write the file out of order; a
	 * {@link RandomAccessFile} is required
	 *
	 * @param board
	 *            What board's BMP to read from.
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
	void readMemory(BMPBoard board, int baseAddress, int size,
			RandomAccessFile dataFile) throws IOException, ProcessException {
		read(board, baseAddress, size, new FileAccumulator(dataFile));
	}

	/**
	 * Read memory into a file.
	 *
	 * @param board
	 *            What board's BMP to read from.
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
	void readMemory(BMPBoard board, int baseAddress, int size, File dataFile)
			throws IOException, ProcessException {
		try (RandomAccessFile s = new RandomAccessFile(dataFile, "rw")) {
			read(board, baseAddress, size, new FileAccumulator(s));
		}
	}
}
