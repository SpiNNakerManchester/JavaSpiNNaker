package uk.ac.manchester.spinnaker.processes;

import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.scp.CheckOKResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.WriteLink;
import uk.ac.manchester.spinnaker.messages.scp.WriteMemory;

/**
 * Write to memory on SpiNNaker.
 */
public class WriteMemoryProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 */
	public WriteMemoryProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
	}

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param numRetries
	 *            The number of times to retry a communication.
	 * @param timeout
	 *            The timeout (in ms) for the communication.
	 * @param numChannels
	 *            The number of parallel communications to support
	 * @param intermediateChannelWaits
	 *            How many parallel communications to launch at once. (??)
	 */
	public WriteMemoryProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			int numRetries, int timeout, int numChannels,
			int intermediateChannelWaits) {
		super(connectionSelector, numRetries, timeout, numChannels,
				intermediateChannelWaits);
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
		 *            The block of data to write with this message.
		 * @return The message to send.
		 */
		T getMessage(int baseAddress, ByteBuffer data);
	}

	/**
	 * Writes memory across a SpiNNaker link from a buffer.
	 *
	 * @param core
	 *            The coordinates of the core of the chip where the link is
	 *            attached to.
	 * @param link
	 *            The link to write over.
	 * @param baseAddress
	 *            Where to start copying to.
	 * @param data
	 *            The buffer of data to be copied. The copied region extends
	 *            from the <i>position</i> (inclusive) to the <i>limit</i>
	 *            (exclusive).
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public void writeLink(HasCoreLocation core, int link, int baseAddress,
			ByteBuffer data) throws IOException, Exception {
		writeMemory(baseAddress, data,
				(addr, bytes) -> new WriteLink(core, link, addr, bytes));
	}

	/**
	 * Writes memory across a SpiNNaker link from an input stream.
	 *
	 * @param core
	 *            The coordinates of the core of the chip where the link is
	 *            attached to.
	 * @param link
	 *            The link to write over.
	 * @param baseAddress
	 *            Where to start copying to.
	 * @param data
	 *            Where to get data from
	 * @param bytesToWrite
	 *            How many bytes should be copied from the stream?
	 * @throws IOException
	 *             If anything goes wrong with networking or the input stream.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public void writeLink(HasCoreLocation core, int link, int baseAddress,
			InputStream data, int bytesToWrite) throws IOException, Exception {
		writeMemory(baseAddress, data, bytesToWrite,
				(addr, bytes) -> new WriteLink(core, link, addr, bytes));
	}

	/**
	 * Writes memory across a SpiNNaker link from a file.
	 *
	 * @param core
	 *            The coordinates of the core of the chip where the link is
	 *            attached to.
	 * @param link
	 *            The link to write over.
	 * @param baseAddress
	 *            Where to start copying to.
	 * @param dataFile
	 *            The file of binary data to be copied. The whole file is
	 *            transferred.
	 * @throws IOException
	 *             If anything goes wrong with networking or access to the file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public void writeLink(HasCoreLocation core, int link, int baseAddress,
			File dataFile) throws IOException, Exception {
		try (InputStream data =
				new BufferedInputStream(new FileInputStream(dataFile))) {
			writeMemory(baseAddress, data, (int) dataFile.length(),
					(addr, bytes) -> new WriteLink(core, link, addr, bytes));
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
	 *            (exclusive).
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public void writeMemory(HasCoreLocation core, int baseAddress,
			ByteBuffer data) throws IOException, Exception {
		writeMemory(baseAddress, data,
				(addr, bytes) -> new WriteMemory(core, addr, bytes));
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
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public void writeMemory(HasCoreLocation core, int baseAddress,
			InputStream data, int bytesToWrite) throws IOException, Exception {
		writeMemory(baseAddress, data, bytesToWrite,
				(addr, bytes) -> new WriteMemory(core, addr, bytes));
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
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public void writeMemory(HasCoreLocation core, int baseAddress,
			File dataFile) throws IOException, Exception {
		try (InputStream data =
				new BufferedInputStream(new FileInputStream(dataFile))) {
			writeMemory(baseAddress, data, (int) dataFile.length(),
					(addr, bytes) -> new WriteMemory(core, addr, bytes));
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
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	protected <T extends SCPRequest<CheckOKResponse>> void writeMemory(
			int baseAddress, ByteBuffer data, MessageProvider<T> msgProvider)
			throws IOException, Exception {
		int offset = data.position();
		int bytesToWrite = data.remaining();
		int writePosition = baseAddress;
		while (bytesToWrite > 0) {
			int bytesToSend = min(bytesToWrite, UDP_MESSAGE_MAX_SIZE);
			ByteBuffer tmp = data.asReadOnlyBuffer();
			tmp.position(offset);
			tmp.limit(offset + bytesToSend);
			sendRequest(msgProvider.getMessage(writePosition, tmp));
			offset += bytesToSend;
			writePosition += bytesToSend;
			bytesToWrite -= bytesToSend;
		}
		finish();
		checkForError();
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
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	protected <T extends SCPRequest<CheckOKResponse>> void writeMemory(
			int baseAddress, InputStream data, int bytesToWrite,
			MessageProvider<T> msgProvider) throws IOException, Exception {
		int writePosition = baseAddress;
		ByteBuffer workingBuffer = allocate(UDP_MESSAGE_MAX_SIZE);
		while (bytesToWrite > 0) {
			int bytesToSend = min(bytesToWrite, UDP_MESSAGE_MAX_SIZE);
			ByteBuffer tmp = workingBuffer.slice();
			bytesToSend = data.read(tmp.array(), 0, bytesToSend);
			if (bytesToSend <= 0) {
				break;
			}
			tmp.limit(bytesToSend);
			sendRequest(msgProvider.getMessage(writePosition, tmp));
			writePosition += bytesToSend;
			bytesToWrite -= bytesToSend;
		}
		finish();
		checkForError();
	}
}
