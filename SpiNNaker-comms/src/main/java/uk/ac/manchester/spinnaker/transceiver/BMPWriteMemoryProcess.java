/*
 * Copyright (c) 2018-2022 The University of Manchester
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
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;

import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.messages.bmp.BMPBoard;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest.BMPResponse;
import uk.ac.manchester.spinnaker.messages.bmp.BMPWriteMemory;

/**
 * Write to memory on SpiNNaker.
 */
class BMPWriteMemoryProcess extends BMPCommandProcess<BMPResponse> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	BMPWriteMemoryProcess(ConnectionSelector<BMPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * A source of messages to write to an address.
	 */
	@FunctionalInterface
	interface MessageProvider {
		/**
		 * Provide a message.
		 *
		 * @param baseAddress
		 *            The base address to write to.
		 * @param data
		 *            The block of data to write with this message.
		 * @return The message to send.
		 */
		BMPWriteMemory getMessage(int baseAddress, ByteBuffer data);
	}

	/**
	 * Writes memory onto a BMP from a buffer.
	 *
	 * @param board
	 *            Which board's BMP to write to.
	 * @param baseAddress
	 *            Where to start copying to.
	 * @param data
	 *            The buffer of data to be copied. The copied region extends
	 *            from the <i>position</i> (inclusive) to the <i>limit</i>
	 *            (exclusive).
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	void writeMemory(BMPBoard board, int baseAddress, ByteBuffer data)
			throws IOException, ProcessException {
		execute((WMIterable) () -> new Iterator<BMPWriteMemory>() {
			int offset = data.position();
			int bytesToWrite = data.remaining();
			int writePosition = baseAddress;

			@Override
			public boolean hasNext() {
				return bytesToWrite > 0;
			}

			@Override
			public BMPWriteMemory next() {
				int bytesToSend = min(bytesToWrite, UDP_MESSAGE_MAX_SIZE);
				ByteBuffer tmp = data.asReadOnlyBuffer();
				tmp.position(offset);
				tmp.limit(offset + bytesToSend);
				try {
					return new BMPWriteMemory(board, writePosition, tmp);
				} finally {
					offset += bytesToSend;
					writePosition += bytesToSend;
					bytesToWrite -= bytesToSend;
				}
			}
		});
	}

	/**
	 * Writes memory onto a BMP from an input stream.
	 *
	 * @param board
	 *            Which board's BMP to write to.
	 * @param baseAddress
	 *            Where to start copying to.
	 * @param data
	 *            Where to get data from
	 * @param bytesToWrite
	 *            How many bytes should be copied from the stream?
	 * @throws IOException
	 *             If anything goes wrong with networking or the input stream.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	void writeMemory(BMPBoard board, int baseAddress, InputStream data,
			int bytesToWrite) throws IOException, ProcessException {
		ByteBuffer workingBuffer = allocate(UDP_MESSAGE_MAX_SIZE);
		execute((WMIterable) () -> new Iterator<BMPWriteMemory>() {
			int bytesRemaining = bytesToWrite;
			int writePosition = baseAddress;
			ByteBuffer tmp;
			int bytesToSend;

			@Override
			public boolean hasNext() {
				if (bytesRemaining < 1) {
					return false;
				}
				try {
					tmp = workingBuffer.slice();
					bytesToSend = data.read(tmp.array(), 0,
							min(bytesRemaining, UDP_MESSAGE_MAX_SIZE));
					tmp.limit(max(0, bytesToSend));
					return bytesToSend > 0;
				} catch (IOException e) {
					return false;
				}
			}

			@Override
			public BMPWriteMemory next() {
				try {
					return new BMPWriteMemory(board, writePosition, tmp);
				} finally {
					writePosition += bytesToSend;
					bytesRemaining -= bytesToSend;
				}
			}
		});
	}

	private interface WMIterable extends Iterable<BMPWriteMemory> {
	}
}
