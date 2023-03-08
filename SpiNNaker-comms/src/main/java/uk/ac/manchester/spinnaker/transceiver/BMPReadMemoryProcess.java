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

import static java.lang.Math.min;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.bmp.ReadBMPMemory;
import uk.ac.manchester.spinnaker.transceiver.Accumulator.BufferAccumulator;
import uk.ac.manchester.spinnaker.transceiver.Accumulator.FileAccumulator;

/** A process for reading memory on a BMP. */
class BMPReadMemoryProcess extends BMPCommandProcess {
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	private <T> T read(BMPBoard board, MemoryLocation address, int size,
			Accumulator<T> accum)
			throws ProcessException, IOException, InterruptedException {
		for (int offset = 0, chunk; offset < size; offset += chunk) {
			chunk = min(size - offset, UDP_MESSAGE_MAX_SIZE);
			accum.add(offset,
					call(new ReadBMPMemory(board, address.add(offset), chunk)));
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void read(BMPBoard board, MemoryLocation baseAddress,
			ByteBuffer receivingBuffer)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	ByteBuffer read(BMPBoard board, MemoryLocation baseAddress, int size)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void read(BMPBoard board, MemoryLocation baseAddress, int size,
			RandomAccessFile dataFile)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void read(BMPBoard board, MemoryLocation baseAddress, int size,
			File dataFile)
			throws IOException, ProcessException, InterruptedException {
		try (var s = new RandomAccessFile(dataFile, "rw")) {
			read(board, baseAddress, size, new FileAccumulator(s));
		}
	}
}
