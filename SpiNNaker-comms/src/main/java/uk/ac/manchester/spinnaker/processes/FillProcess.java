package uk.ac.manchester.spinnaker.processes;

import static java.lang.String.format;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.FillRequest;
import uk.ac.manchester.spinnaker.messages.scp.WriteMemory;

/** A process for filling memory. */
public class FillProcess extends MultiConnectionProcess<SCPConnection> {
	private static final Logger log = getLogger(FillProcess.class);
	private static final int ALIGNMENT = 4;
	private static final int TWO_WORDS = 2 * WORD_SIZE;

	/**
	 * Create.
	 *
	 * @param connectionSelector
	 *            How to choose where to send messages.
	 */
	public FillProcess(ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
	}

	/**
	 * Create.
	 *
	 * @param connectionSelector
	 *            How to choose where to send messages.
	 * @param numRetries
	 *            The number of retries allowed.
	 * @param timeout
	 *            How long to wait for a reply.
	 * @param numChannels
	 *            The number of parallel channels to use.
	 * @param intermediateChannelWaits
	 *            ???
	 */
	public FillProcess(ConnectionSelector<SCPConnection> connectionSelector,
			int numRetries, int timeout, int numChannels,
			int intermediateChannelWaits) {
		super(connectionSelector, numRetries, timeout, numChannels,
				intermediateChannelWaits);
	}

	/**
	 * Fill memory with a value.
	 *
	 * @param chip
	 *            The chip with the memory.
	 * @param baseAddress
	 *            The address in memory to start filling at.
	 * @param data
	 *            The data to fill.
	 * @param size
	 *            The number of bytes to fill.
	 * @param dataType
	 *            The type of data to fill with.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public void fillMemory(HasChipLocation chip, int baseAddress, int data,
			int size, DataType dataType) throws Exception, IOException {
		// Don't do anything if there is nothing to do!
		if (size == 0) {
			return;
		}

		// Check that the data can fill the requested size
		if (size % dataType.size != 0) {
			throw new IllegalArgumentException(format(
					"The size of %d bytes to fill is not divisible by the "
							+ "size of the data of %d bytes",
					size, dataType.size));
		}
		if (baseAddress % ALIGNMENT != 0) {
			log.warn("Unaligned fill starting at {}; please use aligned fills",
					baseAddress);
		}

		// Get a word of data regardless of the type
		ByteBuffer buffer = allocate(TWO_WORDS).order(LITTLE_ENDIAN);
		while (buffer.hasRemaining()) {
			dataType.writeTo(data, buffer);
		}
		buffer.flip();

		generateWriteMessages(chip, baseAddress, size, buffer);

		/*
		 * Wait for all the packets to be confirmed and then check there are no
		 * errors.
		 */
		finish();
		checkForError();
	}

	private void generateWriteMessages(HasChipLocation chip, int base, int size,
			ByteBuffer buffer) throws IOException {
		int toWrite = size;
		int address = base;

		/*
		 * Send the pre-data to make the memory aligned, up to the first word.
		 */
		int extraBytes = (ALIGNMENT - base % ALIGNMENT) % ALIGNMENT;
		if (extraBytes != 0) {
			ByteBuffer preBytes = buffer.duplicate();
			preBytes.limit(extraBytes);
			// Send the preBytes to make the memory aligned
			if (preBytes.hasRemaining()) {
				sendRequest(new WriteMemory(chip, base, preBytes));
			}
			toWrite -= extraBytes;
			address += extraBytes;
		}

		// Fill as much as possible using the bulk operation, FillRequest
		int bulkBytes = (extraBytes != 0) ? size - ALIGNMENT : size;
		if (bulkBytes != 0) {
			sendRequest(new FillRequest(chip, address,
					buffer.getInt(extraBytes), bulkBytes));
			toWrite -= bulkBytes;
			address += bulkBytes;
		}

		/*
		 * Post bytes is the last part of the data from the end of the last
		 * aligned word; send them if required. This uses a WriteMemory
		 */
		if (toWrite != 0) {
			buffer.position(buffer.limit() - base % ALIGNMENT);
			buffer.limit(buffer.position() + toWrite);
			if (buffer.hasRemaining()) {
				sendRequest(new WriteMemory(chip, address, buffer));
			}
		}
	}

	/**
	 * The fill unit.
	 */
	public enum DataType {
		/** Fill by words (4 bytes). */
		WORD(4),
		/** Fill by half words (2 bytes). */
		HALF_WORD(2),
		/** Fill by single bytes. */
		BYTE(1);
		/** The encoding of the fill unit size. */
		public final int size;

		DataType(int value) {
			this.size = value;
		}

		/**
		 * Write a value to the buffer in an appropriate way for this fill unit.
		 *
		 * @param value
		 *            The value to write.
		 * @param buffer
		 *            The buffer to write to.
		 */
		public void writeTo(int value, ByteBuffer buffer) {
			switch (this) {
			case WORD:
				buffer.putInt(value);
				break;
			case HALF_WORD:
				buffer.putShort((short) value);
				break;
			case BYTE:
				buffer.put((byte) value);
				break;
			default:
				// unreachable
				throw new IllegalStateException();
			}
		}
	}
}
