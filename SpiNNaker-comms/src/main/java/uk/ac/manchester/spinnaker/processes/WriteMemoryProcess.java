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
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.scp.CheckOKResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.WriteLink;
import uk.ac.manchester.spinnaker.messages.scp.WriteMemory;
import uk.ac.manchester.spinnaker.selectors.ConnectionSelector;

public class WriteMemoryProcess extends MultiConnectionProcess<SCPConnection> {
	public WriteMemoryProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
	}

	public WriteMemoryProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			int numRetries, int timeout, int numChannels,
			int intermediateChannelWaits) {
		super(connectionSelector, numRetries, timeout, numChannels,
				intermediateChannelWaits);
	}

	@FunctionalInterface
	interface MessageProvider<T> {
		T get(int baseAddress, ByteBuffer data);
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
	 */
	public void writeLink(HasCoreLocation core, int link, int baseAddress,
			File dataFile) throws IOException, Exception {
		try (InputStream data = new BufferedInputStream(
				new FileInputStream(dataFile))) {
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
	 */
	public void writeMemory(HasCoreLocation core, int baseAddress,
			File dataFile) throws IOException, Exception {
		try (InputStream data = new BufferedInputStream(
				new FileInputStream(dataFile))) {
			writeMemory(baseAddress, data, (int) dataFile.length(),
					(addr, bytes) -> new WriteMemory(core, addr, bytes));
		}
	}

	protected <T extends SCPRequest<CheckOKResponse>> void writeMemory(
			int baseAddress, ByteBuffer data, MessageProvider<T> m)
			throws IOException, Exception {
		int offset = data.position();
		int bytesToWrite = data.remaining();
		int writePosition = baseAddress;
		while (bytesToWrite > 0) {
			int bytesToSend = min(bytesToWrite, UDP_MESSAGE_MAX_SIZE);
			ByteBuffer tmp = data.asReadOnlyBuffer();
			tmp.position(offset);
			tmp.limit(offset + bytesToSend);
			sendRequest(m.get(writePosition, tmp));
			offset += bytesToSend;
			writePosition += bytesToSend;
			bytesToWrite -= bytesToSend;
		}
		finish();
		checkForError();
	}

	protected <T extends SCPRequest<CheckOKResponse>> void writeMemory(
			int baseAddress, InputStream data, int bytesToWrite,
			MessageProvider<T> m) throws IOException, Exception {
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
			sendRequest(m.get(writePosition, tmp));
			writePosition += bytesToSend;
			bytesToWrite -= bytesToSend;
		}
		finish();
		checkForError();
	}
}
