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

import static java.lang.Math.max;
import static java.nio.ByteBuffer.allocate;
import static org.apache.commons.io.IOUtils.buffer;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.read;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.sliceUp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.scp.CheckOKResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.WriteLink;
import uk.ac.manchester.spinnaker.messages.scp.WriteMemory;
import uk.ac.manchester.spinnaker.transceiver.exceptions.ProcessException;

/**
 * Write to memory on SpiNNaker.
 */
class WriteMemoryProcess extends TxrxProcess {
	private final String operation;

	/**
	 * @param <Conn>
	 *            The type of connections to use.
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	<Conn extends SCPConnection> WriteMemoryProcess(
			ConnectionSelector<Conn> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
		operation = "Write Memory";
	}

	/**
	 * @param <Conn>
	 *            The type of connections to use.
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	<Conn extends SCPConnection> WriteMemoryProcess(String operation,
			ConnectionSelector<Conn> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
		this.operation = operation;
	}

	/**
	 * @param <Conn>
	 *            The type of connections to use.
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param numChannels
	 *            The number of parallel communications to support
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	<Conn extends SCPConnection> WriteMemoryProcess(
			ConnectionSelector<Conn> connectionSelector, int numChannels,
			RetryTracker retryTracker) {
		super(connectionSelector, SCP_RETRIES, SCP_TIMEOUT, numChannels,
				max(numChannels / 2, 1), retryTracker);
		operation = "Write Memory";
	}

	/**
	 * @param <Conn>
	 *            The type of connections to use.
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param numChannels
	 *            The number of parallel communications to support
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	<Conn extends SCPConnection> WriteMemoryProcess(String operation,
			ConnectionSelector<Conn> connectionSelector, int numChannels,
			RetryTracker retryTracker) {
		super(connectionSelector, SCP_RETRIES, SCP_TIMEOUT, numChannels,
				max(numChannels / 2, 1), retryTracker);
		this.operation = operation;
	}

	/**
	 * A general source of messages to write to an address.
	 *
	 * @param <T>
	 *            The type of messages provided.
	 */
	@FunctionalInterface
	interface MessageProvider<T> {
		/**
		 * Provide a message.
		 *
		 * @param baseAddress
		 *            The base address to write to.
		 * @param data
		 *            The block of data to write with this message. Note that it
		 *            is important that this method does not change the position
		 *            or limit of this buffer; the relevant message constructors
		 *            have this property.
		 * @return The message to send.
		 */
		T getMessage(MemoryLocation baseAddress, ByteBuffer data);
	}

	/**
	 * Writes memory across a SpiNNaker link from a buffer.
	 *
	 * @param core
	 *            The coordinates of the core of the chip where the link is
	 *            attached to.
	 * @param linkDirection
	 *            The link to write over.
	 * @param baseAddress
	 *            Where to start copying to.
	 * @param data
	 *            The buffer of data to be copied. The copied region extends
	 *            from the <i>position</i> (inclusive) to the <i>limit</i>
	 *            (exclusive). The position and limit of the buffer will
	 *            not be updated by this method.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void writeLink(HasCoreLocation core, Direction linkDirection,
			MemoryLocation baseAddress, ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		writeMemoryFlow(baseAddress, data,
				(addr, bytes) -> new WriteLink(operation, core, linkDirection,
						addr, bytes));
	}

	/**
	 * Writes memory across a SpiNNaker link from an input stream.
	 *
	 * @param core
	 *            The coordinates of the core of the chip where the link is
	 *            attached to.
	 * @param linkDirection
	 *            The link to write over.
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
	void writeLink(HasCoreLocation core, Direction linkDirection,
			MemoryLocation baseAddress, InputStream data, int bytesToWrite)
			throws IOException, ProcessException, InterruptedException {
		writeMemoryFlow(baseAddress, data, bytesToWrite,
				(addr, bytes) -> new WriteLink(operation, core, linkDirection,
						addr, bytes));
	}

	/**
	 * Writes memory across a SpiNNaker link from a file.
	 *
	 * @param core
	 *            The coordinates of the core of the chip where the link is
	 *            attached to.
	 * @param linkDirection
	 *            The link to write over.
	 * @param baseAddress
	 *            Where to start copying to.
	 * @param dataFile
	 *            The file of binary data to be copied. The whole file is
	 *            transferred.
	 * @throws IOException
	 *             If anything goes wrong with networking or access to the file.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void writeLink(HasCoreLocation core, Direction linkDirection,
			MemoryLocation baseAddress, File dataFile)
			throws IOException, ProcessException, InterruptedException {
		try (var data = buffer(new FileInputStream(dataFile))) {
			writeMemoryFlow(baseAddress, data, (int) dataFile.length(),
					(addr, bytes) -> new WriteLink(operation, core,
							linkDirection, addr, bytes));
		}
	}

	/**
	 * Writes memory onto a SpiNNaker chip from a buffer.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is to be written
	 *            to.
	 * @param baseAddress
	 *            Where to start copying to.
	 * @param data
	 *            The buffer of data to be copied. The copied region extends
	 *            from the <i>position</i> (inclusive) to the <i>limit</i>
	 *            (exclusive). The position and limit of the buffer will
	 *            not be updated by this method.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void writeMemory(HasCoreLocation core, MemoryLocation baseAddress,
			ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		// Optimise the single-message case
		if (data.remaining() < UDP_MESSAGE_MAX_SIZE) {
			synchronousCall(
					new WriteMemory(operation, core, baseAddress, data));
			return;
		}
		writeMemoryFlow(baseAddress, data,
				(addr, bytes) -> new WriteMemory(operation, core, addr, bytes));
	}

	/**
	 * Writes memory onto a SpiNNaker chip from an input stream.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is to be written
	 *            to.
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
	void writeMemory(HasCoreLocation core, MemoryLocation baseAddress,
			InputStream data, int bytesToWrite)
			throws IOException, ProcessException, InterruptedException {
		writeMemoryFlow(baseAddress, data, bytesToWrite,
				(addr, bytes) -> new WriteMemory(operation, core, addr, bytes));
	}

	/**
	 * Writes memory onto a SpiNNaker chip from a file.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is to be written
	 *            to.
	 * @param baseAddress
	 *            Where to start copying to.
	 * @param dataFile
	 *            The file of binary data to be copied. The whole file is
	 *            transferred.
	 * @throws IOException
	 *             If anything goes wrong with networking or access to the file.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void writeMemory(HasCoreLocation core, MemoryLocation baseAddress,
			File dataFile)
			throws IOException, ProcessException, InterruptedException {
		try (var data = buffer(new FileInputStream(dataFile))) {
			writeMemoryFlow(baseAddress, data, (int) dataFile.length(), (addr,
					bytes) -> new WriteMemory(operation, core, addr, bytes));
		}
	}

	/**
	 * Write to memory.
	 *
	 * @param <T>
	 *            The type of messages to send to do the writing.
	 * @param baseAddress
	 *            The base address to write.
	 * @param data
	 *            The overall block of memory to write
	 * @param msgProvider
	 *            The way to create messages to send to do the writing.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	private <T extends SCPRequest<CheckOKResponse>> void writeMemoryFlow(
			MemoryLocation baseAddress, ByteBuffer data,
			MessageProvider<T> msgProvider)
			throws IOException, ProcessException, InterruptedException {
		var writePosition = baseAddress;
		for (var bb : sliceUp(data, UDP_MESSAGE_MAX_SIZE)) {
			sendRequest(msgProvider.getMessage(writePosition, bb));
			writePosition = writePosition.add(bb.remaining());
		}
		finishBatch();
	}

	/**
	 * Write to memory.
	 *
	 * @param <T>
	 *            The type of messages to send to do the writing.
	 * @param baseAddress
	 *            The base address to write.
	 * @param data
	 *            The stream of data to write.
	 * @param bytesToWrite
	 *            The number of bytes to read from the stream and transfer.
	 * @param msgProvider
	 *            The way to create messages to send to do the writing.
	 * @throws IOException
	 *             If anything goes wrong with networking or the input stream.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	private <T extends SCPRequest<CheckOKResponse>> void writeMemoryFlow(
			MemoryLocation baseAddress, InputStream data, int bytesToWrite,
			MessageProvider<T> msgProvider)
			throws IOException, ProcessException, InterruptedException {
		var writePosition = baseAddress;
		while (bytesToWrite > 0) {
			// One buffer per message; lifetime extends until batch end
			var tmp = read(data, allocate(UDP_MESSAGE_MAX_SIZE), bytesToWrite);
			if (tmp == null) {
				break;
			}
			sendRequest(msgProvider.getMessage(writePosition, tmp));
			writePosition = writePosition.add(tmp.remaining());
			bytesToWrite -= tmp.remaining();
		}
		finishBatch();
	}
}
