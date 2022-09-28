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

import static java.lang.Math.min;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.scp.ReadLink;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.Accumulator.BufferAccumulator;
import uk.ac.manchester.spinnaker.transceiver.Accumulator.FileAccumulator;

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

	/**
	 * Read memory into an accumulator.
	 *
	 * @param <T>
	 *            Type of the result of the accumulator.
	 * @param chip
	 *            What chip has the memory to read from.
	 * @param baseAddress
	 *            where to read from.
	 * @param size
	 *            how much to read
	 * @param a
	 *            The accumulator to receive the data.
	 * @return The result of the accumulator.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	private <T> T readMemory(HasChipLocation chip, MemoryLocation baseAddress,
			int size, Accumulator<T> a) throws IOException, ProcessException {
		for (int offset = 0, chunk; offset < size; offset += chunk) {
			chunk = min(size - offset, UDP_MESSAGE_MAX_SIZE);
			final int thisOffset = offset;
			sendRequest(new ReadMemory(chip, baseAddress.add(offset), chunk),
					response -> a.add(thisOffset, response.data));
		}
		finishBatch();
		return a.finish();
	}

	/**
	 * Read memory over a link into an accumulator.
	 *
	 * @param <T>
	 *            Type of the result of the accumulator.
	 * @param chip
	 *            What chip does the link start at.
	 * @param linkDirection
	 *            The direction of the link to traverse.
	 * @param baseAddress
	 *            where to read from.
	 * @param size
	 *            how much to read
	 * @param a
	 *            The accumulator to receive the data.
	 * @return The result of the accumulator.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	private <T> T readLink(HasChipLocation chip, Direction linkDirection,
			MemoryLocation baseAddress, int size, Accumulator<T> a)
			throws IOException, ProcessException {
		for (int offset = 0, chunk; offset < size; offset += chunk) {
			chunk = min(size - offset, UDP_MESSAGE_MAX_SIZE);
			int thisOffset = offset;
			sendRequest(
					new ReadLink(chip, linkDirection, baseAddress.add(offset),
							chunk),
					response -> a.add(thisOffset, response.data));
		}
		finishBatch();
		return a.finish();
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
	void readLink(HasChipLocation chip, Direction linkDirection,
			MemoryLocation baseAddress, ByteBuffer receivingBuffer)
			throws IOException, ProcessException {
		readLink(chip, linkDirection, baseAddress, receivingBuffer.remaining(),
				new BufferAccumulator(receivingBuffer));
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
	void readMemory(HasChipLocation chip, MemoryLocation baseAddress,
			ByteBuffer receivingBuffer) throws IOException, ProcessException {
		readMemory(chip, baseAddress, receivingBuffer.remaining(),
				new BufferAccumulator(receivingBuffer));
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
	ByteBuffer readLink(HasChipLocation chip, Direction linkDirection,
			MemoryLocation baseAddress, int size)
			throws IOException, ProcessException {
		return readLink(chip, linkDirection, baseAddress, size,
				new BufferAccumulator(size));
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
	ByteBuffer readMemory(HasChipLocation chip, MemoryLocation baseAddress,
			int size) throws IOException, ProcessException {
		return readMemory(chip, baseAddress, size, new BufferAccumulator(size));
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
	void readLink(HasChipLocation chip, Direction linkDirection,
			MemoryLocation baseAddress, int size, RandomAccessFile dataFile)
			throws IOException, ProcessException {
		readLink(chip, linkDirection, baseAddress, size,
				new FileAccumulator(dataFile));
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
	void readMemory(HasChipLocation chip, MemoryLocation baseAddress, int size,
			RandomAccessFile dataFile) throws IOException, ProcessException {
		readMemory(chip, baseAddress, size, new FileAccumulator(dataFile));
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
	void readLink(HasChipLocation chip, Direction linkDirection,
			MemoryLocation baseAddress, int size, File dataFile)
			throws IOException, ProcessException {
		try (var s = new RandomAccessFile(dataFile, "rw")) {
			readLink(chip, linkDirection, baseAddress, size,
					new FileAccumulator(s));
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
	void readMemory(HasChipLocation chip, MemoryLocation baseAddress, int size,
			File dataFile) throws IOException, ProcessException {
		try (var s = new RandomAccessFile(dataFile, "rw")) {
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
	void readMemory(BufferManagerStorage.Region region,
			BufferManagerStorage storage)
			throws IOException, ProcessException, StorageException {
		var buffer = new byte[region.size];
		readMemory(region.core.asChipLocation(), region.startAddress,
				region.size, new BufferAccumulator(buffer));
		storage.appendRecordingContents(region, buffer);
	}
}
