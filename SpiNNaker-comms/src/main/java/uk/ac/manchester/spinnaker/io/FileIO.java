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

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.transceiver.FillDataType;
import uk.ac.manchester.spinnaker.utils.Slice;

/** A file input/output interface to match the MemoryIO interface. */
public class FileIO extends BaseIO {
	/** The file to write to. */
	@GuardedBy("itself")
	private final RandomAccessFile file;

	/**
	 * @param file
	 *            The file handle or file name to write to
	 * @param startOffset
	 *            The start offset into the file
	 * @param endOffset
	 *            The end offset from the start of the file
	 * @throws IOException
	 *             If anything goes wrong opening the file.
	 */
	public FileIO(File file, MemoryLocation startOffset,
			MemoryLocation endOffset) throws IOException {
		super(startOffset, endOffset);
		this.file = new RandomAccessFile(file, "rw");
	}

	/**
	 * @param file
	 *            The file handle or file name to write to
	 * @param startOffset
	 *            The start offset into the file
	 * @param endOffset
	 *            The end offset from the start of the file
	 */
	private FileIO(FileIO file, MemoryLocation startOffset,
			MemoryLocation endOffset) {
		super(startOffset, endOffset);
		this.file = file.ref();
	}

	private RandomAccessFile ref() {
		synchronized (file) {
			return file;
		}
	}

	@Override
	public void close() throws Exception {
		synchronized (file) {
			file.close();
		}
	}

	@Override
	public FileIO get(int slice) {
		if (slice < 0 || slice >= size()) {
			throw new ArrayIndexOutOfBoundsException(slice);
		}
		return new FileIO(this, start.add(slice), start.add(slice + 1));
	}

	@Override
	public FileIO get(Slice slice) {
		return get(slice, (from, to) -> new FileIO(this, from, to));
	}

	@Override
	public boolean isClosed() {
		synchronized (file) {
			return !file.getChannel().isOpen();
		}
	}

	@Override
	public void flush() {
		// Do nothing
	}

	@Override
	byte[] doRead(int numBytes) throws IOException {
		var data = new byte[numBytes];
		synchronized (file) {
			file.seek(current.address());
			file.readFully(data, 0, numBytes);
		}
		return data;
	}

	@Override
	void doWrite(byte[] data, int from, int len) throws IOException {
		synchronized (file) {
			file.seek(current.address());
			file.write(data, from, len);
		}
	}

	@Override
	void doFill(int value, FillDataType type, int len) throws IOException {
		var b = allocate(len).order(LITTLE_ENDIAN);
		while (b.hasRemaining()) {
			type.writeTo(value, b);
		}
		doWrite(b.array(), 0, b.position());
	}
}
