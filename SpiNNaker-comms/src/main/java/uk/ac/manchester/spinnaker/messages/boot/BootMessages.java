package uk.ac.manchester.spinnaker.messages.boot;

import static java.lang.Integer.reverseBytes;
import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.System.arraycopy;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.util.Collections.singleton;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.concat;
import static uk.ac.manchester.spinnaker.messages.Constants.MS_PER_S;
import static uk.ac.manchester.spinnaker.messages.boot.BootOpCode.FLOOD_FILL_BLOCK;
import static uk.ac.manchester.spinnaker.messages.boot.BootOpCode.FLOOD_FILL_CONTROL;
import static uk.ac.manchester.spinnaker.messages.boot.BootOpCode.FLOOD_FILL_START;
import static uk.ac.manchester.spinnaker.messages.boot.SystemVariableBootValues.BOOT_VARIABLE_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.boot_signature;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.is_root_chip;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.unix_timestamp;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition;

/** Represents a set of boot messages to be sent to boot the board. */
public class BootMessages {
	private static final int BOOT_MESSAGE_DATA_WORDS = 256;
	private static final int BOOT_MESSAGE_DATA_BYTES = BOOT_MESSAGE_DATA_WORDS
			* 4;
	private static final int BOOT_IMAGE_MAX_BYTES = 32 * 1024;
	private static final int BOOT_STRUCT_REPLACE_OFFSET = 384;
	private static final int BOOT_STRUCT_REPLACE_LENGTH = 128;
	private static final String BOOT_IMAGE = "scamp.boot";

	private final ByteBuffer bootData;
	private final int numDataPackets;

	private static void initFlags(SystemVariableBootValues bootVars) {
		int currentTime = (int) (System.currentTimeMillis() / MS_PER_S);
		bootVars.setValue(unix_timestamp, currentTime);
		bootVars.setValue(boot_signature, currentTime);
		bootVars.setValue(is_root_chip, 1);
	}

	private BootMessages(SystemVariableBootValues bootVariables,
			Map<SystemVariableDefinition, Object> extraBootValues) {
		initFlags(bootVariables);
		if (extraBootValues != null) {
			for (Entry<SystemVariableDefinition, Object> entry : extraBootValues
					.entrySet()) {
				bootVariables.setValue(entry.getKey(), entry.getValue());
			}
		}
		// NB: This data is BIG endian!
		ByteBuffer buffer = allocate(BOOT_VARIABLE_SIZE).order(BIG_ENDIAN);
		bootVariables.addToBuffer(buffer);
		bootData = readBootImage(getClass().getResource(BOOT_IMAGE));
		arraycopy(buffer.array(), 0, bootData.array(),
				BOOT_STRUCT_REPLACE_OFFSET, BOOT_STRUCT_REPLACE_LENGTH);
		numDataPackets = (int) ceil(
				bootData.limit() / (float) BOOT_MESSAGE_DATA_BYTES);
	}

	private static final int WORD_SIZE = 4;
	private static ByteBuffer readBootImage(URL bootImage) {
		// NB: This data is BIG endian!
		ByteBuffer buffer = allocate(BOOT_IMAGE_MAX_BYTES + WORD_SIZE)
				.order(BIG_ENDIAN);
		buffer.position(0);

		try (DataInputStream is = new DataInputStream(bootImage.openStream())) {
			while (true) {
				// NB: Byte-swap the data from the file!
				buffer.putInt(reverseBytes(is.readInt()));
			}
		} catch (EOFException e) {
			// EOF signifies end of file; nothing to do here
		} catch (IOException e) {
			throw new Error("could not load boot image: " + BOOT_IMAGE, e);
		}

		// Sanity-check the results
		if (buffer.position() > BOOT_IMAGE_MAX_BYTES) {
			throw new Error(format(
					"The boot file is too big at %d bytes"
							+ " (only files up to 32KiB are acceptable)",
					buffer.position()));
		}
		if (buffer.position() % WORD_SIZE != 0) {
			// This ought to be unreachable...
			throw new Error(format(
					"The boot file size of %d bytes must be divisible by 4",
					buffer.position()));
		}
		if (buffer.position() < BOOT_STRUCT_REPLACE_OFFSET
				+ BOOT_STRUCT_REPLACE_LENGTH) {
			throw new Error(format(
					"The boot file size of %d bytes is not large enough to"
							+ " contain the boot configuration",
					buffer.position()));
		}

		buffer.limit(buffer.position());
		return buffer;
	}

	/** Builds the boot messages needed to boot the SpiNNaker machine. */
	public BootMessages() {
		this(new SystemVariableBootValues(), null);
	}

	/**
	 * Builds the boot messages needed to boot the SpiNNaker machine.
	 *
	 * @param boardVersion
	 *            The version of the board to be booted
	 */
	public BootMessages(int boardVersion) {
		this(SystemVariableBootValues.get(boardVersion), null);
	}

	/**
	 * Builds the boot messages needed to boot the SpiNNaker machine.
	 *
	 * @param extraBootValues
	 *            Any additional values to be set during boot
	 */
	public BootMessages(
			Map<SystemVariableDefinition, Object> extraBootValues) {
		this(new SystemVariableBootValues(), extraBootValues);
	}

	/**
	 * Builds the boot messages needed to boot the SpiNNaker machine.
	 *
	 * @param boardVersion
	 *            The version of the board to be booted
	 * @param extraBootValues
	 *            Any additional values to be set during boot
	 */
	public BootMessages(int boardVersion,
			Map<SystemVariableDefinition, Object> extraBootValues) {
		this(SystemVariableBootValues.get(boardVersion), extraBootValues);
	}

	private BootMessage getBootMessage(int blockID) {
		ByteBuffer buffer = bootData.duplicate();
		buffer.position(blockID * BOOT_MESSAGE_DATA_BYTES);
		buffer.limit(buffer.position()
				+ min(buffer.remaining(), BOOT_MESSAGE_DATA_BYTES));
		assert buffer.hasRemaining() : "buffer must have space left";
		return new BootMessage(FLOOD_FILL_BLOCK, 1, 0, 0, buffer);
	}

	/** @return a stream of message to be sent. */
	public Stream<BootMessage> getMessages() {
		Stream<BootMessage> first = singleton(new BootMessage(
				FLOOD_FILL_START, 0, 0, numDataPackets - 1)).stream();
		Stream<BootMessage> mid =
				range(0, numDataPackets).mapToObj(this::getBootMessage);
		Stream<BootMessage> last = singleton(
				new BootMessage(FLOOD_FILL_CONTROL, 1, 0, 0)).stream();
		return concat(first, concat(mid, last));
	}
}
