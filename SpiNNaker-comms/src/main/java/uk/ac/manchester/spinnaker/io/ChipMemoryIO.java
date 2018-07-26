package uk.ac.manchester.spinnaker.io;

import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;

import java.io.EOFException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
	/**
	 * A set of ChipMemoryIO objects that have been created, indexed by
	 * transceiver, x and y (thus two transceivers might not see the same
	 * buffered memory)
	 */
	private static Map<Transceiver, Map<ChipLocation, ChipMemoryIO>> existing = new WeakHashMap<>();

	static ChipMemoryIO get_chip_memory_io(Transceiver transceiver,
			HasChipLocation chip) {
		Map<ChipLocation, ChipMemoryIO> map = existing.get(transceiver);
		if (map == null) {
			map = new HashMap<>();
			existing.put(transceiver, map);
		}
		ChipLocation key = chip.asChipLocation();
		if (!map.containsKey(key)) {
			map.put(key, new ChipMemoryIO(transceiver, chip, 0x60000000, 256));
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
	private int current_address;

	/** The current pointer where the next buffered write will occur */
	private int write_address;

	/** The write buffer size */
	private final int buffer_size;

	/** The write buffer bytearray */
	private final ByteBuffer write_buffer;

	ChipMemoryIO(Transceiver transceiver, ChipLocation chip) {
		this(transceiver, chip, 0x60000000, 256);
	}

	/**
	 * @param transceiver
	 *            The transceiver to read and write with
	 * @param x
	 *            The x-coordinate of the chip to write to
	 * @param y
	 *            The y-coordinate of the chip to write to
	 * @param base_address
	 *            The lowest address that can be written
	 * @param buffer_size
	 *            The size of the write buffer to improve efficiency
	 */
	ChipMemoryIO(Transceiver transceiver, HasChipLocation chip,
			int base_address, int buffer_size) {
		this.transceiver = new WeakReference<>(transceiver);
		core = chip.getScampCore().asCoreLocation();
		current_address = base_address;
		this.buffer_size = buffer_size;
		write_address = base_address;
		write_buffer = allocate(buffer_size).order(ByteOrder.LITTLE_ENDIAN);
	}

	private Transceiver txrx() throws IOException {
		Transceiver t = transceiver.get();
		if (t == null) {
			throw new EOFException();
		}
		return t;
	}

	/** Force the writing of the current write buffer */
	void flush_write_buffer() throws IOException, Exception {
		if (write_buffer.position() > 0) {
			Transceiver t = hold;
			if (t == null) {
				t = txrx();
			}
			ByteBuffer b = write_buffer.duplicate();
			b.flip();
			t.writeMemory(core, write_address, b);
			write_address += write_buffer.position();
			write_buffer.position(0);
		}
		hold = null;
	}

	/** Return the current absolute address within the region */
	int getCurrentAddress() {
		return current_address;
	}

	/** Seek to a position within the region */
	void setCurrentAddress(int address) throws IOException, Exception {
		flush_write_buffer();
		current_address = address;
		write_address = address;
	}

	/**
	 * Read a number of bytes
	 *
	 * @param n_bytes
	 *            The number of bytes to read
	 */
	byte[] read(int n_bytes) throws IOException, Exception {
		if (n_bytes == 0) {
			return new byte[0];
		}

		flush_write_buffer();
		ByteBuffer data = txrx().readMemory(core, current_address, n_bytes);
		current_address += n_bytes;
		write_address = current_address;
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
		if (n_bytes >= buffer_size) {
			flush_write_buffer();
			t.writeMemory(core, current_address, data);
			current_address += n_bytes;
			write_address = current_address;
		} else {
			hold = t;
			int n_bytes_to_copy = min(n_bytes,
					buffer_size - write_buffer.position());
			write_buffer.put(data, 0, n_bytes_to_copy);
			current_address += n_bytes_to_copy;
			n_bytes -= n_bytes_to_copy;
			if (!write_buffer.hasRemaining()) {
				flush_write_buffer();
			}
			if (n_bytes > 0) {
				write_buffer.clear();
				write_buffer.put(data, n_bytes_to_copy, n_bytes);
				current_address += n_bytes;
			}
		}
	}

	/**
	 * Fill the memory with repeated data
	 *
	 * @param repeat_value
	 *            The value to repeat
	 * @param bytes_to_fill
	 *            Number of bytes to fill from current position
	 * @param data_type
	 *            The type of the repeat value
	 */
	void fill(int repeat_value, int bytes_to_fill,
			FillProcess.DataType data_type) throws IOException, Exception {
		Transceiver t = txrx();
		flush_write_buffer();
		t.fillMemory(core, current_address, repeat_value, bytes_to_fill,
				data_type);
		current_address += bytes_to_fill;
	}
}