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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.read;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.sliceUp;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.scp.SendMCDataRequest;

/**
 * Write to memory on SpiNNaker via multicast (data in only).
 */
class WriteMemoryByMulticastProcess extends TxrxProcess {

	/** Timeout for a write request; longer as the write can take some time. */
	private static final int TIMEOUT = 10000;

	/** The number of simultaneous messages that can be in progress. */
	private static final int N_CHANNELS = 8;

	private static final int UDP_MESSAGE_MAX_WORDS =
			UDP_MESSAGE_MAX_SIZE / WORD_SIZE;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	WriteMemoryByMulticastProcess(
			ConnectionSelector<? extends SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, SCP_RETRIES, TIMEOUT, N_CHANNELS,
				N_CHANNELS - 1,	retryTracker);
	}

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param numChannels
	 *            The number of parallel communications to support
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	WriteMemoryByMulticastProcess(
			ConnectionSelector<? extends SCPConnection> connectionSelector,
			int numChannels, RetryTracker retryTracker) {
		super(connectionSelector, SCP_RETRIES, TIMEOUT, numChannels,
				max(numChannels / 2, 1), retryTracker);
	}

	/**
	 * Write to memory.
	 *
	 * @param core
	 *            The location to send the message to.
	 * @param targetCore
	 *            The target to write the data to.
	 * @param baseAddress
	 *            The base address to write.
	 * @param data
	 *            The overall block of memory to write
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	public void writeMemory(
			HasCoreLocation core, HasCoreLocation targetCore,
			MemoryLocation baseAddress, ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		var writePosition = baseAddress;
		for (var bb : sliceUp(data, UDP_MESSAGE_MAX_SIZE)) {
			sendRequest(new SendMCDataRequest(core, targetCore, writePosition,
					bb));
			writePosition = writePosition.add(bb.remaining());
		}
		finishBatch();
	}

	/**
	 * Write to memory.
	 *
	 * @param core
	 *            The location to send the message to.
	 * @param targetCore
	 *            The target to write the data to.
	 * @param baseAddress
	 *            The base address to write.
	 * @param data
	 *            The stream of data to write.
	 * @throws IOException
	 *             If anything goes wrong with networking or the input stream.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	public void writeMemory(
			HasCoreLocation core, HasCoreLocation targetCore,
			MemoryLocation baseAddress, InputStream data)
			throws IOException, ProcessException, InterruptedException {
		var writePosition = baseAddress;
		while (true) {
			// One buffer per message; lifetime extends until batch end
			var tmp = read(data, allocate(UDP_MESSAGE_MAX_SIZE),
					UDP_MESSAGE_MAX_SIZE);
			if (tmp == null) {
				break;
			}
			sendRequest(new SendMCDataRequest(core, targetCore, writePosition,
					tmp));
			writePosition = writePosition.add(tmp.remaining());
		}
		finishBatch();
	}

	/**
	 * Write to memory reading from a stream of data that includes context
	 * switches.  Each section is a "header" of memory address (integer),
	 * chip x and y (each a short) and the number of words, followed by the
	 * words themselves.  The number of words does not have to be short enough
	 * to fit in a packet; this will be managed by this function.
	 *
	 * @param core
	 *            The location to send the messages to.
	 * @param data
	 *            The stream of data to write.
	 * @throws IOException
	 *             If anything goes wrong with networking or the input stream.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	public void writeMemoryStream(HasCoreLocation core, InputStream data)
			throws IOException, ProcessException, InterruptedException {
		DataInputStream input = new DataInputStream(data);

		// Keep a buffer to be sent, along with the initial send details
		var nextBuffer = allocate(UDP_MESSAGE_MAX_SIZE).order(LITTLE_ENDIAN);
		MemoryLocation nextAddress = null;
		ChipLocation nextChip = null;
		int nextNWords = -1;
		byte[] buffer = new byte[UDP_MESSAGE_MAX_SIZE];

		while (true) {

			// Read the next address and location to send to
			MemoryLocation baseAddress;
			try {
				int addr = input.readInt();
				baseAddress = new MemoryLocation(addr);
			} catch (EOFException e) {
				break;
			}
			int x = input.readShort();
			int y = input.readShort();
			var targetChip = new ChipLocation(x, y);
			var nWords = input.readInt();

			// Either we are at the start of the buffer, or there isn't enough
			// space for the address info and some data (we need at least as
			// much data as the header size to make it worth it)
			if (nextBuffer.position() == 0 ||
					nextBuffer.remaining() < 2 * 3 * WORD_SIZE) {

				// If the buffer has some words in it, send it now
				if (nextBuffer.position() > 0) {
					nextBuffer.flip();
					sendRequest(new SendMCDataRequest(core, nextChip,
							nextAddress, nextNWords, nextBuffer));
					nextBuffer = allocate(UDP_MESSAGE_MAX_SIZE)
							.order(LITTLE_ENDIAN);
				}
				nextAddress = baseAddress;
				nextChip = targetChip;

				// However the size depends on the space available!
				nextNWords = min(nWords, UDP_MESSAGE_MAX_WORDS);
			} else {
				// We have enough space, so write the change of context to the
				// buffer
				var nWordsAvailable = (nextBuffer.remaining() / WORD_SIZE) - 3;
				nextBuffer.putInt(baseAddress.address);
				nextBuffer.putShort((short) targetChip.getY());
				nextBuffer.putShort((short) targetChip.getX());

				// Once again, the size depends on the space available!
				nextBuffer.putInt(min(nWords, nWordsAvailable));
			}

			var writePosition = baseAddress;
			var nBytesRemaining = nWords * WORD_SIZE;
			while (nBytesRemaining > 0) {

				// Read data into the buffer
				if ((nBytesRemaining / WORD_SIZE) * WORD_SIZE != nBytesRemaining) {
					throw new IOException("Remaining bytes " + nBytesRemaining
							+ " is not a multiple of word size " + WORD_SIZE);
				}
				var nextReadSize = min(nBytesRemaining, nextBuffer.remaining());
				input.readFully(buffer, 0, nextReadSize);
				nextBuffer.put(buffer, 0, nextReadSize);

				writePosition = writePosition.add(nextReadSize);
				nBytesRemaining -= nextReadSize;

				// If the buffer is full, send it and update the header values
				// for the potential next send
				if (nextBuffer.remaining() == 0) {
					nextBuffer.flip();
					sendRequest(new SendMCDataRequest(core, nextChip,
							nextAddress, nextNWords, nextBuffer));
					nextBuffer = allocate(UDP_MESSAGE_MAX_SIZE)
							.order(LITTLE_ENDIAN);
					nextChip = targetChip;
					nextAddress = writePosition;
					nextNWords = min(nBytesRemaining / WORD_SIZE,
							UDP_MESSAGE_MAX_WORDS);
				}
			}
		}

		// We might still have one last send to do...
		if (nextBuffer.remaining() > 0) {
			nextBuffer.flip();
			sendRequest(new SendMCDataRequest(core, nextChip,
					nextAddress, nextNWords, nextBuffer));
		}

		finishBatch();
	}
}
