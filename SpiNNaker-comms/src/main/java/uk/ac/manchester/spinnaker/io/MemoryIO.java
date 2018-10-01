package uk.ac.manchester.spinnaker.io;

import java.io.EOFException;
import java.io.IOException;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.processes.FillProcess.DataType;
import uk.ac.manchester.spinnaker.processes.Process.Exception;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.utils.Slice;

/** A file-like object for reading and writing memory. */
public class MemoryIO implements AbstractIO {
	/** The transceiver for speaking to the machine. */
	private final ChipMemoryIO io;
	/** The start address of the region to write to. */
	private final int startAddress;
	/** The current pointer where read and writes are taking place. */
	private int currentAddress;
	/** The end of the region to write to. */
	private final int endAddress;

	/**
	 * @param transceiver
	 *            The transceiver to read and write with
	 * @param chip
	 *            The coordinates of the chip to write to
	 * @param startAddress
	 *            The start address of the region to write to
	 * @param endAddress
	 *            The end address of the region to write to. This is the first
	 *            address just outside the region
	 */
	public MemoryIO(Transceiver transceiver, HasChipLocation chip,
			int startAddress, int endAddress) {
		if (startAddress >= endAddress) {
			throw new IllegalArgumentException(
					"start address must precede end address");
		}
		io = ChipMemoryIO.getInstance(transceiver, chip);
		this.startAddress = startAddress;
		this.currentAddress = startAddress;
		this.endAddress = endAddress;
	}

	private MemoryIO(ChipMemoryIO io, int startAddress, int endAddress) {
		this.io = io;
		this.startAddress = startAddress;
		this.currentAddress = startAddress;
		this.endAddress = endAddress;
	}

	@Override
	public void close() throws Exception, IOException {
		synchronized (io) {
			io.flushWriteBuffer();
		}
	}

	@Override
	public int size() {
		return endAddress - startAddress;
	}

	@Override
	public MemoryIO get(int slice) throws IOException {
		if (slice < 0 || slice >= size()) {
			throw new ArrayIndexOutOfBoundsException(slice);
		}
		return new MemoryIO(io, startAddress + slice, startAddress + slice + 1);
	}

	@Override
	public MemoryIO get(Slice slice) throws IOException {
		int from = startAddress;
		int to = endAddress;
		if (slice.start != null) {
			if (slice.start < 0) {
				from = endAddress + slice.start;
			} else {
				from += slice.start;
			}
		}
		if (slice.stop != null) {
			if (slice.stop < 0) {
				to = endAddress + slice.stop;
			} else {
				to += slice.stop;
			}
		}
		if (from < startAddress || from > endAddress) {
			throw new ArrayIndexOutOfBoundsException(slice.start);
		}
		if (to < startAddress || to > endAddress) {
			throw new ArrayIndexOutOfBoundsException(slice.stop);
		}
		if (from == to) {
			throw new ArrayIndexOutOfBoundsException(
					"zero-sized regions are not supported");
		}
		return new MemoryIO(io, from, to);
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public void flush() throws IOException, Exception {
		synchronized (io) {
			io.flushWriteBuffer();
		}
	}

	@Override
	public void seek(int numBytes) throws IOException {
		int position = startAddress + numBytes;
		if (position < startAddress || position > endAddress) {
			throw new IllegalArgumentException(
					"Attempt to seek to a position of " + position
							+ " which is outside of the region");
		}
		currentAddress = position;
	}

	@Override
	public int tell() throws IOException {
		return currentAddress - startAddress;
	}

	@Override
	public int getAddress() {
		return currentAddress;
	}

	@Override
	public byte[] read(Integer numBytes) throws IOException, Exception {
		int size = (numBytes != null && numBytes >= 0) ? numBytes
				: (endAddress - currentAddress);
		if (currentAddress + size > endAddress) {
			throw new EOFException();
		}
		byte[] data;
		synchronized (io) {
			io.setCurrentAddress(currentAddress);
			data = io.read(size);
		}
		currentAddress += size;
		return data;
	}

	@Override
	public int write(byte[] data) throws IOException, Exception {
		int size = data.length;
		if (currentAddress + size > endAddress) {
			throw new EOFException();
		}
		synchronized (io) {
			io.setCurrentAddress(currentAddress);
			io.write(data);
		}
		currentAddress += size;
		return size;
	}

	@Override
	public void fill(int value, Integer size, DataType type)
			throws IOException, Exception {
		int len = (size == null) ? endAddress - currentAddress : size;
		if (currentAddress + len > endAddress) {
			throw new EOFException();
		}
		if (len % type.size != 0) {
			throw new IllegalArgumentException("The size of " + len
					+ " bytes to fill is not divisible by the size of"
					+ " the data of " + type.size + " bytes");
		}
		synchronized (io) {
			io.setCurrentAddress(currentAddress);
			io.fill(value, len, type);
		}
		currentAddress += len;
	}
}
