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

import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.util.Optional.empty;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Optional;

import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.Constants;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest.BMPResponse;
import uk.ac.manchester.spinnaker.messages.bmp.WriteSerialFlash;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * Write to memory on a BMP.
 */
class BMPWriteSerialFlashProcess extends BMPCommandProcess<BMPResponse> {
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
	 */
	void write(BMPBoard board, MemoryLocation baseAddress, ByteBuffer data)
			throws IOException, ProcessException {
		execute(new BMPWriteSFIterator(board, baseAddress, data.remaining()) {
			private int offset = data.position();

			@Override
			Optional<ByteBuffer> prepareSendBuffer(int chunkSize) {
				var buffer = data.asReadOnlyBuffer();
				buffer.position(offset);
				buffer.limit(offset + chunkSize);
				offset += chunkSize;
				return Optional.of(buffer);
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
	void write(BMPBoard board, MemoryLocation baseAddress, InputStream data,
			int bytesToWrite) throws IOException, ProcessException {
		var exn = new ValueHolder<IOException>();
		var workingBuffer = allocate(UDP_MESSAGE_MAX_SIZE);
		execute(new BMPWriteSFIterator(board, baseAddress, bytesToWrite) {
			@Override
			Optional<ByteBuffer> prepareSendBuffer(int chunkSize) {
				try {
					var buffer = workingBuffer.duplicate();
					// After this, chunkSize is REAL chunk size or -1
					chunkSize = data.read(buffer.array(), 0, chunkSize);
					if (chunkSize < 1) {
						// Read failed to generate anything we want to send
						return empty();
					}
					buffer.limit(chunkSize);
					return Optional.of(buffer);
				} catch (IOException e) {
					exn.setValue(e); // Smuggle the exception out!
					return empty();
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
 * This is also an iterable, albeit a one-shot iterable.
 *
 * @author Donal Fellows
 */
abstract class BMPWriteSFIterator
		implements Iterator<WriteSerialFlash>, Iterable<WriteSerialFlash> {
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
	BMPWriteSFIterator(BMPBoard board, MemoryLocation address, int size) {
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
	 * @return The wrapped chunk, or {@link Optional#empty()} if no chunk
	 *         available.
	 */
	@UsedInJavadocOnly(Constants.class)
	abstract Optional<ByteBuffer> prepareSendBuffer(int plannedSize);

	@Override
	public final boolean hasNext() {
		if (sizeRemaining < 1) {
			return false;
		}
		var bb = prepareSendBuffer(min(sizeRemaining, UDP_MESSAGE_MAX_SIZE));
		sendBuffer = bb.orElse(null);
		return bb.isPresent();
	}

	@Override
	public final WriteSerialFlash next() {
		int chunkSize = sendBuffer.remaining();
		try {
			return new WriteSerialFlash(board, address, sendBuffer);
		} finally {
			address = address.add(chunkSize);
			sizeRemaining -= chunkSize;
		}
	}

	@Override
	public Iterator<WriteSerialFlash> iterator() {
		return this;
	}
}
