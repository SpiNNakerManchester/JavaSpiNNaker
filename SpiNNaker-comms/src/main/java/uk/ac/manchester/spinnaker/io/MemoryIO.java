package uk.ac.manchester.spinnaker.io;

import java.io.EOFException;
import java.io.IOException;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.processes.FillProcess.DataType;
import uk.ac.manchester.spinnaker.processes.Process.Exception;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/** A file-like object for reading and writing memory. */
public class MemoryIO implements AbstractIO {
	/** The transceiver for speaking to the machine */
	private final ChipMemoryIO chip_memory_io;
	/** The start address of the region to write to */
	private final int start_address;
	/** The current pointer where read and writes are taking place */
	private int current_address;
	/** The end of the region to write to. */
	private final int end_address;

	/**
	 * @param transceiver
	 *            The transceiver to read and write with
	 * @param chip
	 *            The coordinates of the chip to write to
	 * @param start_address
	 *            The start address of the region to write to
	 * @param end_address
	 *            The end address of the region to write to. This is the first
	 *            address just outside the region
	 */
	public MemoryIO(Transceiver transceiver, HasChipLocation chip,
			int start_address, int end_address) {
		if (start_address >= end_address) {
			throw new IllegalArgumentException(
					"start address must precede end address");
		}
		chip_memory_io = ChipMemoryIO.get_chip_memory_io(transceiver, chip);
		this.start_address = start_address;
		this.current_address = start_address;
		this.end_address = end_address;
	}

	private MemoryIO(ChipMemoryIO chip_memory_io, int start_address,
			int end_address) {
		this.chip_memory_io = chip_memory_io;
		this.start_address = start_address;
		this.current_address = start_address;
		this.end_address = end_address;
	}

	@Override
	public void close() throws Exception, IOException {
		synchronized (chip_memory_io) {
			chip_memory_io.flush_write_buffer();
		}
	}

	@Override
	public int size() {
		return end_address - start_address;
	}

	@Override
	public MemoryIO get(int slice) throws IOException {
		if (slice < 0 || slice >= size()) {
			throw new ArrayIndexOutOfBoundsException(slice);
		}
		return new MemoryIO(chip_memory_io, start_address + slice,
				start_address + slice + 1);
	}

	@Override
	public MemoryIO get(Slice slice) throws IOException {
		int from = start_address;
		int to = end_address;
		if (slice.start != null) {
			if (slice.start < 0) {
				from = end_address + slice.start;
			} else {
				from += slice.start;
			}
		}
		if (slice.stop != null) {
			if (slice.stop < 0) {
				to = end_address + slice.stop;
			} else {
				to += slice.stop;
			}
		}
		if (from < start_address || from > end_address) {
			throw new ArrayIndexOutOfBoundsException(slice.start);
		}
		if (to < start_address || to > end_address) {
			throw new ArrayIndexOutOfBoundsException(slice.stop);
		}
		if (from == to) {
			throw new ArrayIndexOutOfBoundsException(
					"zero-sized regions are not supported");
		}
		return new MemoryIO(chip_memory_io, from, to);
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public void flush() throws IOException, Exception {
		synchronized (chip_memory_io) {
			chip_memory_io.flush_write_buffer();
		}
	}

	@Override
	public void seek(int numBytes) throws IOException {
		int position = start_address + numBytes;
		if (position < start_address || position > end_address) {
			throw new IllegalArgumentException(
					"Attempt to seek to a position of " + position
							+ " which is outside of the region");
		}
		current_address = position;
	}

	@Override
	public int tell() throws IOException {
		return current_address - start_address;
	}

	@Override
	public int getAddress() {
		return current_address;
	}

	@Override
	public byte[] read(Integer numBytes) throws IOException, Exception {
		int bytes_to_read = (numBytes != null && numBytes >= 0) ? numBytes
				: (end_address - current_address);
		if (current_address + bytes_to_read > end_address) {
			throw new EOFException();
		}
		byte[] data;
		synchronized (chip_memory_io) {
			chip_memory_io.setCurrentAddress(current_address);
			data = chip_memory_io.read(bytes_to_read);
		}
		current_address += bytes_to_read;
		return data;
	}

	@Override
	public int write(byte[] data) throws IOException, Exception {
		int n_bytes = data.length;
		if (current_address + n_bytes > end_address) {
			throw new EOFException();
		}
		synchronized (chip_memory_io) {
			chip_memory_io.setCurrentAddress(current_address);
			chip_memory_io.write(data);
		}
		current_address += n_bytes;
		return n_bytes;
	}

	@Override
	public void fill(int repeat_value, Integer bytes_to_fill,
			DataType data_type) throws IOException, Exception {
		int len = (bytes_to_fill == null) ? end_address - current_address
				: bytes_to_fill;
		if (current_address + len > end_address) {
			throw new EOFException();
		}
		if (len % data_type.value != 0) {
			throw new IllegalArgumentException("The size of " + len
					+ " bytes to fill is not divisible by the size of"
					+ " the data of " + data_type.value + " bytes");
		}
		synchronized (chip_memory_io) {
			chip_memory_io.setCurrentAddress(current_address);
			chip_memory_io.fill(repeat_value, len, data_type);
		}
		current_address += len;
	}

}