/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.read;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;

import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.Constants;
import uk.ac.manchester.spinnaker.messages.bmp.WriteSerialFlash;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * Write to memory on a BMP.
 */
class BMPWriteSerialFlashProcess extends BMPCommandProcess {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	BMPWriteSerialFlashProcess(
			ConnectionSelector<BMPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
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
	 *            (exclusive). This method is not obligated to preserve either
	 *            the position or the limit, though the current implementation
	 *            does so. The contents of the buffer will not be modified.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void write(BMPBoard board, MemoryLocation baseAddress, ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		execute(new BMPWriteSFIterable(board, baseAddress, data.remaining()) {
			private int offset = data.position();

			@Override
			ByteBuffer prepareSendBuffer(int chunkSize) {
				var buffer = data.slice(offset, chunkSize).order(LITTLE_ENDIAN);
				offset += chunkSize;
				return buffer;
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void write(BMPBoard board, MemoryLocation baseAddress, InputStream data,
			int bytesToWrite)
			throws IOException, ProcessException, InterruptedException {
		var exn = new ValueHolder<IOException>();
		var workingBuffer = allocate(UDP_MESSAGE_MAX_SIZE);
		execute(new BMPWriteSFIterable(board, baseAddress, bytesToWrite) {
			@Override
			ByteBuffer prepareSendBuffer(int chunkSize) {
				try {
					return read(data, workingBuffer, chunkSize);
				} catch (IOException e) {
					exn.setValue(e); // Smuggle the exception out!
					return null;
				}
			}
		});
		if (exn.getValue() != null) {
			throw exn.getValue();
		}
	}
}

/**
 * Helper for writing a stream of chunks. Allows us to construct the chunks one
 * at a time on demand. The complexity is because chunk construction is
 * permitted to fail!
 * <p>
 * This can only produce an iterator <em>once</em>.
 *
 * @author Donal Fellows
 */
abstract class BMPWriteSFIterable implements Iterable<WriteSerialFlash> {
	private final BMPBoard board;

	private int sizeRemaining;

	private MemoryLocation address;

	private ByteBuffer sendBuffer;

	/**
	 * @param board
	 *            Where the write messages are going.
	 * @param address
	 *            Where the writes start at.
	 * @param size
	 *            What size of memory will be written.
	 */
	BMPWriteSFIterable(BMPBoard board, MemoryLocation address, int size) {
		this.board = board;
		this.address = address;
		this.sizeRemaining = size;
	}

	/**
	 * Get the next chunk.
	 *
	 * @param plannedSize
	 *            What size the chunk should be. Up to
	 *            {@link Constants#UDP_MESSAGE_MAX_SIZE}.
	 * @return The chunk, or {@code null} if no chunk available.
	 */
	@UsedInJavadocOnly(Constants.class)
	abstract ByteBuffer prepareSendBuffer(int plannedSize);

	@Override
	public Iterator<WriteSerialFlash> iterator() {
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				if (sizeRemaining < 1) {
					return false;
				}
				sendBuffer = prepareSendBuffer(
						min(sizeRemaining, UDP_MESSAGE_MAX_SIZE));
				return sendBuffer != null;
			}

			@Override
			public WriteSerialFlash next() {
				int chunkSize = sendBuffer.remaining();
				try {
					return new WriteSerialFlash(board, address, sendBuffer);
				} finally {
					address = address.add(chunkSize);
					sizeRemaining -= chunkSize;
				}
			}
		};
	}
}
