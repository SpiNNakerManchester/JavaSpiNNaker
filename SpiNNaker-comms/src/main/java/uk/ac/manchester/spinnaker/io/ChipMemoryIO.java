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
package uk.ac.manchester.spinnaker.io;

import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.EOFException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.transceiver.FillDataType;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/** A file-like object for the memory of a chip. */
final class ChipMemoryIO {
	/**
	 * Start of unbuffered access to SDRAM. Writes will block until they have
	 * completed.
	 * <p>
	 * <blockquote> Buffered SDRAM means that writes go through a write buffer.
	 * Unbuffered means that they go directly to SDRAM. Reads are unaffected in
	 * general. If you are writing lots of data, it is unlikely to matter much
	 * since the write buffer is limited in size. Here you probably want to use
	 * Unbuffered anyway, as it will then block until the write is definitely
	 * done. Using Buffered writing means that the write may or may not have
	 * happened at the time of the response. </blockquote>
	 */
	private static final int UNBUFFERED_SDRAM_START = 0x60000000;

	/**
	 * A set of ChipMemoryIO objects that have been created, indexed by
	 * transceiver, x and y (thus two transceivers might not see the same
	 * buffered memory).
	 */
	private static Map<Transceiver, Map<ChipLocation, ChipMemoryIO>> existing =
			new WeakHashMap<>();

	/**
	 * Get the instance for a particular transceiver and chip.
	 *
	 * @param transceiver
	 *            The transceiver.
	 * @param chip
	 *            The chip.
	 * @return The access interface to the chip's memory.
	 */
	static ChipMemoryIO getInstance(Transceiver transceiver,
			HasChipLocation chip) {
		Map<ChipLocation, ChipMemoryIO> map =
				existing.computeIfAbsent(transceiver, x -> new HashMap<>());
		return map.computeIfAbsent(chip.asChipLocation(),
				k -> new ChipMemoryIO(transceiver, chip, UNBUFFERED_SDRAM_START,
						UDP_MESSAGE_MAX_SIZE));
	}

	/** The transceiver for speaking to the machine. */
	private final WeakReference<Transceiver> transceiver;

	/**
	 * A strong reference to the transceiver, held while there is data to flush.
	 */
	private Transceiver hold;

	/** The coordinates of the chip to communicate with. */
	private final CoreLocation core;

	/** The current pointer where read and writes are taking place. */
	private int currentAddress;

	/** The current pointer where the next buffered write will occur. */
	private int writeAddress;

	/** The write buffer. */
	private final ByteBuffer writeBuffer;

	/**
	 * @param transceiver
	 *            The transceiver to read and write with
	 * @param chip
	 *            The coordinates of the chip to write to
	 * @param baseAddress
	 *            The lowest address that can be written
	 * @param bufferSize
	 *            The size of the write buffer to improve efficiency
	 */
	private ChipMemoryIO(Transceiver transceiver, HasChipLocation chip,
			int baseAddress, int bufferSize) {
		this.transceiver = new WeakReference<>(transceiver);
		core = chip.getScampCore().asCoreLocation();
		currentAddress = baseAddress;
		writeAddress = baseAddress;
		writeBuffer = allocate(bufferSize).order(LITTLE_ENDIAN);
	}

	private Transceiver txrx() throws IOException {
		Transceiver t = transceiver.get();
		if (t == null) {
			throw new EOFException();
		}
		return t;
	}

	/**
	 * Force the writing of the current write buffer.
	 *
	 * @throws IOException
	 *             If the OS has a problem sending or receiving a message.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	void flushWriteBuffer() throws IOException, ProcessException {
		if (writeBuffer.position() > 0) {
			Transceiver t = hold;
			if (t == null) {
				t = txrx();
			}
			ByteBuffer b = writeBuffer.duplicate();
			b.flip();
			t.writeMemory(core, writeAddress, b);
			writeAddress += writeBuffer.position();
			writeBuffer.position(0);
		}
		hold = null;
	}

	/**
	 * Seek to a position within the region.
	 *
	 * @param address
	 *            The address to set.
	 * @throws IOException
	 *             If the OS has a problem sending or receiving a message.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	void setCurrentAddress(int address) throws IOException, ProcessException {
		flushWriteBuffer();
		currentAddress = address;
		writeAddress = address;
	}

	/**
	 * Read a number of bytes.
	 *
	 * @return The bytes that have been read.
	 * @param numBytes
	 *            The number of bytes to read
	 * @throws IOException
	 *             If the OS has a problem sending or receiving a message.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	byte[] read(int numBytes) throws IOException, ProcessException {
		if (numBytes == 0) {
			return new byte[0];
		}

		flushWriteBuffer();
		ByteBuffer data = txrx().readMemory(core, currentAddress, numBytes);
		currentAddress += numBytes;
		writeAddress = currentAddress;
		if (data.position() == 0 && data.hasArray()) {
			return data.array();
		}
		byte[] bytes = new byte[data.remaining()];
		data.get(bytes);
		return bytes;
	}

	/**
	 * Write some data.
	 *
	 * @param data
	 *            The data to write
	 * @throws IOException
	 *             If the OS has a problem sending or receiving a message.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	void write(byte[] data) throws IOException, ProcessException {
		int numBytes = data.length;

		Transceiver t = txrx();
		if (numBytes >= writeBuffer.limit()) {
			flushWriteBuffer();
			t.writeMemory(core, currentAddress, data);
			currentAddress += numBytes;
			writeAddress = currentAddress;
		} else {
			hold = t;
			int chunkSize = min(numBytes, writeBuffer.remaining());
			writeBuffer.put(data, 0, chunkSize);
			currentAddress += chunkSize;
			numBytes -= chunkSize;
			if (!writeBuffer.hasRemaining()) {
				flushWriteBuffer();
			}
			if (numBytes > 0) {
				writeBuffer.clear();
				writeBuffer.put(data, chunkSize, numBytes);
				currentAddress += numBytes;
			}
		}
	}

	/**
	 * Fill the memory with repeated data.
	 *
	 * @param value
	 *            The value to repeat
	 * @param size
	 *            Number of bytes to fill from current position
	 * @param type
	 *            The type of the repeat value
	 * @throws IOException
	 *             If the OS has a problem sending or receiving a message.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	void fill(int value, int size, FillDataType type)
			throws IOException, ProcessException {
		Transceiver t = txrx();
		flushWriteBuffer();
		t.fillMemory(core, currentAddress, value, size, type);
		currentAddress += size;
	}
}
