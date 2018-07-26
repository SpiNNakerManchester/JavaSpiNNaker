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
import uk.ac.manchester.spinnaker.processes.FillProcess;
import uk.ac.manchester.spinnaker.processes.Process.Exception;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/** A file-like object for the memory of a chip */
class ChipMemoryIO {
	/** TODO What is this value _really?_ */
	private static final int SDRAM_START = 0x60000000;
	/**
	 * A set of ChipMemoryIO objects that have been created, indexed by
	 * transceiver, x and y (thus two transceivers might not see the same
	 * buffered memory)
	 */
	private static Map<Transceiver, Map<ChipLocation, ChipMemoryIO>> existing = new WeakHashMap<>();

	static ChipMemoryIO getInstance(Transceiver transceiver,
			HasChipLocation chip) {
		Map<ChipLocation, ChipMemoryIO> map = existing.get(transceiver);
		if (map == null) {
			map = new HashMap<>();
			existing.put(transceiver, map);
		}
		ChipLocation key = chip.asChipLocation();
		if (!map.containsKey(key)) {
			map.put(key, new ChipMemoryIO(transceiver, chip, SDRAM_START,
					UDP_MESSAGE_MAX_SIZE));
		}
		return map.get(key);
	}

	/** The transceiver for speaking to the machine */
	private final WeakReference<Transceiver> transceiver;
	/**
	 * A strong reference to the transceiver, held while there is data to flush.
	 */
	private Transceiver hold;

	/** The coordinates of the chip to communicate with */
	private final CoreLocation core;

	/** The current pointer where read and writes are taking place */
	private int currentAddress;

	/** The current pointer where the next buffered write will occur */
	private int writeAddress;

	/** The write buffer */
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
	ChipMemoryIO(Transceiver transceiver, HasChipLocation chip, int baseAddress,
			int bufferSize) {
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

	/** Force the writing of the current write buffer */
	void flushWriteBuffer() throws IOException, Exception {
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

	/** Seek to a position within the region */
	void setCurrentAddress(int address) throws IOException, Exception {
		flushWriteBuffer();
		currentAddress = address;
		writeAddress = address;
	}

	/**
	 * Read a number of bytes
	 *
	 * @param numBytes
	 *            The number of bytes to read
	 */
	byte[] read(int numBytes) throws IOException, Exception {
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
	 * Write some data
	 *
	 * @param data
	 *            The data to write
	 */
	void write(byte[] data) throws IOException, Exception {
		int n_bytes = data.length;

		Transceiver t = txrx();
		if (n_bytes >= writeBuffer.limit()) {
			flushWriteBuffer();
			t.writeMemory(core, currentAddress, data);
			currentAddress += n_bytes;
			writeAddress = currentAddress;
		} else {
			hold = t;
			int n_bytes_to_copy = min(n_bytes, writeBuffer.remaining());
			writeBuffer.put(data, 0, n_bytes_to_copy);
			currentAddress += n_bytes_to_copy;
			n_bytes -= n_bytes_to_copy;
			if (!writeBuffer.hasRemaining()) {
				flushWriteBuffer();
			}
			if (n_bytes > 0) {
				writeBuffer.clear();
				writeBuffer.put(data, n_bytes_to_copy, n_bytes);
				currentAddress += n_bytes;
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
	 */
	void fill(int value, int size, FillProcess.DataType type)
			throws IOException, Exception {
		Transceiver t = txrx();
		flushWriteBuffer();
		t.fillMemory(core, currentAddress, value, size, type);
		currentAddress += size;
	}
}