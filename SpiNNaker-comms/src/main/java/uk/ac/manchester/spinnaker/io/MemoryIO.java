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

import java.io.IOException;

import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.transceiver.FillDataType;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.utils.Slice;

/** A file-like object for reading and writing memory. */
public class MemoryIO extends BaseIO {
	/** The transceiver for speaking to the machine. */
	@GuardedBy("itself")
	private final ChipMemoryIO io;

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
			MemoryLocation startAddress, MemoryLocation endAddress) {
		super(startAddress, endAddress);
		io = ChipMemoryIO.getInstance(transceiver, chip);
	}

	private MemoryIO(MemoryIO mem, MemoryLocation startAddress,
			MemoryLocation endAddress) {
		super(startAddress, endAddress);
		this.io = mem.ref();
	}

	@Override
	public void close() throws ProcessException, IOException {
		synchronized (io) {
			io.flushWriteBuffer();
		}
	}

	private ChipMemoryIO ref() {
		synchronized (io) {
			return io;
		}
	}

	@Override
	public MemoryIO get(int slice) throws IOException {
		if (slice < 0 || slice >= size()) {
			throw new ArrayIndexOutOfBoundsException(slice);
		}
		return new MemoryIO(this, start.add(slice), start.add(slice + 1));
	}

	@Override
	public MemoryIO get(Slice slice) throws IOException {
		return get(slice, (from, to) -> new MemoryIO(this, from, to));
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public void flush() throws IOException, ProcessException {
		synchronized (io) {
			io.flushWriteBuffer();
		}
	}

	@Override
	byte[] doRead(int size) throws IOException, ProcessException {
		synchronized (io) {
			io.setCurrentAddress(current);
			return io.read(size);
		}
	}

	@Override
	void doWrite(byte[] data, int from, int len)
			throws IOException, ProcessException {
		synchronized (io) {
			io.setCurrentAddress(current);
			io.write(data);
		}
	}

	@Override
	void doFill(int value, FillDataType type, int len)
			throws IOException, ProcessException {
		synchronized (io) {
			io.setCurrentAddress(current);
			io.fill(value, len, type);
		}
	}
}
