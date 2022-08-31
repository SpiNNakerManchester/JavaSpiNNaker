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
package uk.ac.manchester.spinnaker.messages.boot;

import static java.lang.Integer.reverseBytes;
import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.concat;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.boot.SystemVariableBootValues.BOOT_VARIABLE_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.boot_signature;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.is_root_chip;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.unix_timestamp;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.stream.Stream;

import uk.ac.manchester.spinnaker.machine.MachineVersion;
import uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition;

/** Represents a set of boot messages to be sent to boot the board. */
public class BootMessages {
	/**
	 * The (maximum) number of data words in an individual boot data message.
	 */
	static final int BOOT_MESSAGE_DATA_WORDS = 256;

	private static final int BOOT_MESSAGE_DATA_BYTES =
			BOOT_MESSAGE_DATA_WORDS * WORD_SIZE;

	private static final int BOOT_IMAGE_MAX_BYTES = 32 * 1024;

	private static final int BOOT_STRUCT_REPLACE_OFFSET = 384;

	private static final int BOOT_STRUCT_REPLACE_LENGTH = 128;

	private static final String BOOT_IMAGE = "scamp.boot";

	private final ByteBuffer bootData;

	private final int numDataPackets;

	private static SystemVariableBootValues initFlags(
			SystemVariableBootValues bootVars) {
		var specific = isNull(bootVars) ? new SystemVariableBootValues()
				: new SystemVariableBootValues(bootVars);
		int currentTime = (int) (currentTimeMillis() / MSEC_PER_SEC);
		specific.setValue(unix_timestamp, currentTime);
		specific.setValue(boot_signature, currentTime);
		specific.setValue(is_root_chip, 1);
		return specific;
	}

	private BootMessages(SystemVariableBootValues bootVariables,
			Map<SystemVariableDefinition, Object> extraBootValues) {
		bootVariables = initFlags(bootVariables);
		if (nonNull(extraBootValues)) {
			extraBootValues.forEach(bootVariables::setValue);
		}
		bootData = readBootImage(getClass().getResource(BOOT_IMAGE));
		injectBootVariableBlock(bootVariables);
		numDataPackets =
				(int) ceil(bootData.limit() / (float) BOOT_MESSAGE_DATA_BYTES);
	}

	private void injectBootVariableBlock(
			SystemVariableBootValues bootVariables) {
		// NB: Endian shenanigans!
		var buffer = allocate(BOOT_VARIABLE_SIZE).order(LITTLE_ENDIAN);
		bootVariables.addToBuffer(buffer);
		buffer.position(0);
		for (int i = 0; i < BOOT_STRUCT_REPLACE_LENGTH / WORD_SIZE; i++) {
			bootData.putInt(BOOT_STRUCT_REPLACE_OFFSET + i * WORD_SIZE,
					buffer.getInt());
		}
	}

	private static ByteBuffer readBootImage(URL bootImage) {
		// NB: This data is BIG endian!
		var buffer =
				allocate(BOOT_IMAGE_MAX_BYTES + WORD_SIZE).order(BIG_ENDIAN);

		try (var is = new DataInputStream(bootImage.openStream())) {
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
		this((SystemVariableBootValues) null, null);
	}

	/**
	 * Builds the boot messages needed to boot the SpiNNaker machine.
	 *
	 * @param boardVersion
	 *            The version of the board to be booted
	 */
	public BootMessages(MachineVersion boardVersion) {
		this(SystemVariableBootValues.get(boardVersion), null);
	}

	/**
	 * Builds the boot messages needed to boot the SpiNNaker machine.
	 *
	 * @param extraBootValues
	 *            Any additional values to be set during boot
	 */
	public BootMessages(Map<SystemVariableDefinition, Object> extraBootValues) {
		this((SystemVariableBootValues) null, extraBootValues);
	}

	/**
	 * Builds the boot messages needed to boot the SpiNNaker machine.
	 *
	 * @param boardVersion
	 *            The version of the board to be booted
	 * @param extraBootValues
	 *            Any additional values to be set during boot
	 */
	public BootMessages(MachineVersion boardVersion,
			Map<SystemVariableDefinition, Object> extraBootValues) {
		this(SystemVariableBootValues.get(boardVersion), extraBootValues);
	}

	private BootMessage getBootMessage(int blockID) {
		/*
		 * Compute the data in the payload; note that this is a pure byte
		 * sequence right now so endianness checks are moot.
		 */
		var buffer = bootData.duplicate();
		buffer.position(blockID * BOOT_MESSAGE_DATA_BYTES);
		buffer.limit(buffer.position()
				+ min(buffer.remaining(), BOOT_MESSAGE_DATA_BYTES));

		// Make the message
		return new BootDataBlock(blockID, buffer);
	}

	/** @return a stream of messages to be sent. */
	public Stream<BootMessage> getMessages() {
		// The bookending control messages
		var start = new StartOfBootMessages(numDataPackets);
		var finish = new EndOfBootMessages();

		// Concatenate everything in the right order
		var imageMessages = range(0, numDataPackets)
				.mapToObj(this::getBootMessage);
		return concat(Stream.of(start),
				concat(imageMessages, Stream.of(finish)));
	}
}
