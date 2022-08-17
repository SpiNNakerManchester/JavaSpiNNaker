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

import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.BufferedInputStream;
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
class WriteMemoryFloodProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	WriteMemoryFloodProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	private static final float BPW = 4.0F;

	private static int numBlocks(int numBytes) {
		return (int) ceil(ceil(numBytes / BPW) / UDP_MESSAGE_MAX_SIZE);
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
	 */
	void writeMemory(byte nearestNeighbourID, MemoryLocation baseAddress,
			ByteBuffer data) throws IOException, ProcessException {
		data = data.asReadOnlyBuffer();
		int numBytes = data.remaining();
		synchronousCall(
				new FloodFillStart(nearestNeighbourID, numBlocks(numBytes)));

		int blockID = 0;
		while (numBytes > 0) {
			int chunk = min(numBytes, UDP_MESSAGE_MAX_SIZE);
			var tmp = data.duplicate();
			tmp.limit(tmp.position() + chunk);
			sendRequest(new FloodFillData(nearestNeighbourID, blockID,
					baseAddress, tmp));
			blockID++;
			numBytes -= chunk;
			baseAddress = baseAddress.add(chunk);
			data.position(data.position() + chunk);
		}
		finish();
		checkForError();

		synchronousCall(new FloodFillEnd(nearestNeighbourID));
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
	 */
	void writeMemory(byte nearestNeighbourID, MemoryLocation baseAddress,
			InputStream dataStream, int numBytes)
			throws IOException, ProcessException {
		synchronousCall(
				new FloodFillStart(nearestNeighbourID, numBlocks(numBytes)));

		int blockID = 0;
		while (numBytes > 0) {
			int chunk = min(numBytes, UDP_MESSAGE_MAX_SIZE);
			// Allocate a new array each time; assume message hold a ref to it
			var buffer = new byte[chunk];
			chunk = dataStream.read(buffer);
			if (chunk <= 0) {
				break;
			}
			sendRequest(new FloodFillData(nearestNeighbourID, blockID,
					baseAddress, buffer, 0, chunk));
			blockID++;
			numBytes -= chunk;
			baseAddress = baseAddress.add(chunk);
		}
		finish();
		checkForError();

		synchronousCall(new FloodFillEnd(nearestNeighbourID));
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
	 */
	void writeMemory(byte nearestNeighbourID, MemoryLocation baseAddress,
			File dataFile) throws IOException, ProcessException {
		try (var s = new BufferedInputStream(new FileInputStream(dataFile))) {
			writeMemory(nearestNeighbourID, baseAddress, s,
					(int) dataFile.length());
		}
	}
}
