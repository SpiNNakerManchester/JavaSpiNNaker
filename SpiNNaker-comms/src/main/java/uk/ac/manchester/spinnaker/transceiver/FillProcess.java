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
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.String.format;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.scp.FillRequest;
import uk.ac.manchester.spinnaker.messages.scp.WriteMemory;
import uk.ac.manchester.spinnaker.transceiver.exceptions.ProcessException;

/** A process for filling memory. */
class FillProcess extends TxrxProcess {
	private static final Logger log = getLogger(FillProcess.class);

	private static final int ALIGNMENT = 4;

	private static final int TWO_WORDS = 2 * WORD_SIZE;

	/**
	 * Create.
	 *
	 * @param connectionSelector
	 *            How to choose where to send messages.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	FillProcess(ConnectionSelector<? extends SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Fill memory with a value.
	 *
	 * @param chip
	 *            The chip with the memory.
	 * @param baseAddress
	 *            The address in memory to start filling at.
	 * @param data
	 *            The data to fill.
	 * @param size
	 *            The number of bytes to fill.
	 * @param dataType
	 *            The type of data to fill with.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 * @throws IllegalArgumentException
	 *             If the size doesn't match the alignment of the data type.
	 */
	void fillMemory(HasChipLocation chip, MemoryLocation baseAddress, int data,
			int size, FillDataType dataType)
			throws ProcessException, IOException, InterruptedException {
		// Don't do anything if there is nothing to do!
		if (size == 0) {
			return;
		}

		// Check that the data can fill the requested size
		if (size % dataType.size != 0) {
			throw new IllegalArgumentException(format(
					"The size of %d bytes to fill is not divisible by the "
							+ "size of the data of %d bytes",
					size, dataType.size));
		}
		if (!baseAddress.isAligned()) {
			log.warn("Unaligned fill starting at {}; please use aligned fills",
					baseAddress);
		}

		// Get a word of data regardless of the type
		var buffer = allocate(TWO_WORDS).order(LITTLE_ENDIAN);
		while (buffer.hasRemaining()) {
			dataType.writeTo(data, buffer);
		}
		buffer.flip();

		generateWriteMessages(chip, baseAddress, size, buffer);

		/*
		 * Wait for all the packets to be confirmed and then check there are no
		 * errors.
		 */
		finishBatch();
	}

	private void generateWriteMessages(HasChipLocation chip,
			MemoryLocation base, int size, ByteBuffer buffer)
			throws IOException, InterruptedException {
		int toWrite = size;
		var address = base;

		// Send the pre-data to make the memory aligned, up to the first word.
		int extraBytes = (ALIGNMENT - base.subWordAlignment()) % ALIGNMENT;
		if (extraBytes != 0) {
			var preBytes = buffer.duplicate();
			preBytes.limit(extraBytes);
			// Send the preBytes to make the memory aligned
			if (preBytes.hasRemaining()) {
				sendRequest(new WriteMemory(chip, base, preBytes));
			}
			toWrite -= extraBytes;
			address = address.add(extraBytes);
		}

		// Fill as much as possible using the bulk operation, FillRequest
		int bulkBytes = (extraBytes != 0) ? size - ALIGNMENT : size;
		if (bulkBytes != 0) {
			sendRequest(new FillRequest(chip, address,
					buffer.getInt(extraBytes), bulkBytes));
			toWrite -= bulkBytes;
			address.add(bulkBytes);
		}

		/*
		 * Post bytes is the last part of the data from the end of the last
		 * aligned word; send them if required. This uses a WriteMemory
		 */
		if (toWrite != 0) {
			buffer.position(buffer.limit() - base.subWordAlignment());
			buffer.limit(buffer.position() + toWrite);
			if (buffer.hasRemaining()) {
				sendRequest(new WriteMemory(chip, address, buffer));
			}
		}
	}
}
