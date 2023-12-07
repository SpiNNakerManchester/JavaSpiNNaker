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
import static java.nio.ByteBuffer.allocate;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.read;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.sliceUp;

import java.io.DataInputStream;
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
		var baseAddress = new MemoryLocation(input.readInt());
		var targetChip = new ChipLocation(input.readShort(), input.readShort());
		var nWords = input.readInt();

		var writePosition = baseAddress;
		var nBytesRemaining = nWords * WORD_SIZE;
		while (nBytesRemaining > 0) {
			// One buffer per message; lifetime extends until batch end
			var tmp = read(input, allocate(UDP_MESSAGE_MAX_SIZE),
					nBytesRemaining);
			if (tmp == null) {
				break;
			}
			sendRequest(new SendMCDataRequest(core, targetChip, writePosition,
					tmp));
			writePosition = writePosition.add(tmp.remaining());
			nBytesRemaining -= tmp.remaining();
		}
		finishBatch();
	}
}
