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

import static java.nio.ByteBuffer.allocate;
import static org.apache.commons.io.IOUtils.buffer;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.read;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.sliceUp;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.scp.FloodFillData;
import uk.ac.manchester.spinnaker.messages.scp.FloodFillEnd;
import uk.ac.manchester.spinnaker.messages.scp.FloodFillStart;

/** A process for writing memory on multiple SpiNNaker chips at once. */
final class WriteMemoryFloodProcess extends TxrxProcess {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	WriteMemoryFloodProcess(
			ConnectionSelector<? extends SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	private static int numBlocks(int numBytes) {
		return ceildiv(numBytes, UDP_MESSAGE_MAX_SIZE);
	}

	/**
	 * Flood fills memory with data from a buffer.
	 *
	 * @param nearestNeighbourID
	 *            The ID of the fill
	 * @param baseAddress
	 *            Where the data is to be written to
	 * @param data
	 *            The data, from the <i>position</i> (inclusive) to the
	 *            <i>limit</i> (exclusive)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void writeMemory(byte nearestNeighbourID, MemoryLocation baseAddress,
			ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		int numBytes = data.remaining();
		call(new FloodFillStart(nearestNeighbourID, numBlocks(numBytes)));

		int blockID = 0;
		for (var slice : sliceUp(data, UDP_MESSAGE_MAX_SIZE)) {
			sendRequest(new FloodFillData(nearestNeighbourID, blockID++,
					baseAddress, slice));
			baseAddress = baseAddress.add(slice.remaining());
		}
		finishBatch();

		call(new FloodFillEnd(nearestNeighbourID));
	}

	/**
	 * Flood fills memory with data from an input stream.
	 *
	 * @param nearestNeighbourID
	 *            The ID of the fill
	 * @param baseAddress
	 *            Where the data is to be written to
	 * @param dataStream
	 *            The place to get the data from.
	 * @param numBytes
	 *            The number of bytes to read. Be aware that you can specify a
	 *            number larger than the number of bytes actually available; if
	 *            you do so, the fill will terminate early and this may cause
	 *            problems.
	 * @throws IOException
	 *             If anything goes wrong with networking or the input stream.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void writeMemory(byte nearestNeighbourID, MemoryLocation baseAddress,
			InputStream dataStream, int numBytes)
			throws IOException, ProcessException, InterruptedException {
		call(new FloodFillStart(nearestNeighbourID, numBlocks(numBytes)));

		int blockID = 0;
		while (numBytes > 0) {
			// Allocate a new buffer each time; assume message holds ref to it
			var tmp =
					read(dataStream, allocate(UDP_MESSAGE_MAX_SIZE), numBytes);
			if (tmp == null) {
				break;
			}
			sendRequest(new FloodFillData(nearestNeighbourID, blockID,
					baseAddress, tmp));
			blockID++;
			numBytes -= tmp.remaining();
			baseAddress = baseAddress.add(tmp.remaining());
		}
		finishBatch();

		call(new FloodFillEnd(nearestNeighbourID));
	}

	/**
	 * Flood fills memory with data from a file.
	 *
	 * @param nearestNeighbourID
	 *            The ID of the fill
	 * @param baseAddress
	 *            Where the data is to be written to
	 * @param dataFile
	 *            The data in a file, which will be fully transferred.
	 * @throws IOException
	 *             If anything goes wrong with networking or access to the file.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void writeMemory(byte nearestNeighbourID, MemoryLocation baseAddress,
			File dataFile)
			throws IOException, ProcessException, InterruptedException {
		try (var s = buffer(new FileInputStream(dataFile))) {
			writeMemory(nearestNeighbourID, baseAddress, s,
					(int) dataFile.length());
		}
	}
}
