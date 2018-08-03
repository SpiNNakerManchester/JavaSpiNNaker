package uk.ac.manchester.spinnaker.processes;

import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.messages.scp.FloodFillData;
import uk.ac.manchester.spinnaker.messages.scp.FloodFillEnd;
import uk.ac.manchester.spinnaker.messages.scp.FloodFillStart;

/** A process for writing memory on multiple SpiNNaker chips at once. */
public class WriteMemoryFloodProcess
		extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 */
	public WriteMemoryFloodProcess(
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
	public WriteMemoryFloodProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			int numRetries, int timeout, int numChannels,
			int intermediateChannelWaits) {
		super(connectionSelector, numRetries, timeout, numChannels,
				intermediateChannelWaits);
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
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public void writeMemory(byte nearestNeighbourID, int baseAddress,
			ByteBuffer data) throws IOException, Exception {
		data = data.asReadOnlyBuffer();
		int numBytes = data.remaining();
		synchronousCall(
				new FloodFillStart(nearestNeighbourID, numBlocks(numBytes)));

		int blockID = 0;
		while (numBytes > 0) {
			int chunk = min(numBytes, UDP_MESSAGE_MAX_SIZE);
			ByteBuffer tmp = data.duplicate();
			tmp.limit(tmp.position() + chunk);
			sendRequest(new FloodFillData(nearestNeighbourID, blockID,
					baseAddress, tmp));
			blockID++;
			numBytes -= chunk;
			baseAddress += chunk;
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
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public void writeMemory(byte nearestNeighbourID, int baseAddress,
			InputStream dataStream, int numBytes)
			throws IOException, Exception {
		synchronousCall(
				new FloodFillStart(nearestNeighbourID, numBlocks(numBytes)));

		int blockID = 0;
		while (numBytes > 0) {
			int chunk = min(numBytes, UDP_MESSAGE_MAX_SIZE);
			// Allocate a new array each time; assume message hold a ref to it
			byte[] buffer = new byte[chunk];
			chunk = dataStream.read(buffer);
			if (chunk <= 0) {
				break;
			}
			sendRequest(new FloodFillData(nearestNeighbourID, blockID,
					baseAddress, buffer, 0, chunk));
			blockID++;
			numBytes -= chunk;
			baseAddress += chunk;
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
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public void writeMemory(byte nearestNeighbourID, int baseAddress,
			File dataFile) throws IOException, Exception {
		try (InputStream s =
				new BufferedInputStream(new FileInputStream(dataFile))) {
			writeMemory(nearestNeighbourID, baseAddress, s,
					(int) dataFile.length());
		}
	}
}
