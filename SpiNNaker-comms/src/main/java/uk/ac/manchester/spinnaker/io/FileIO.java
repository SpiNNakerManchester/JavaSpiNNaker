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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.transceiver.processes.FillProcess.DataType;
import uk.ac.manchester.spinnaker.utils.Slice;

/** A file input/output interface to match the MemoryIO interface. */
public class FileIO implements AbstractIO {
	/** The file to write to. */
	private final RandomAccessFile file;

	/** The current offset in the file. */
	private int currentOffset;

	/** The start offset in the file. */
	private int startOffset;

	/** The end offset in the file. */
	private int endOffset;

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
	public FileIO(File file, int startOffset, int endOffset)
			throws IOException {
		this(new RandomAccessFile(file, "rw"), startOffset, endOffset);
	}

	/**
	 * @param file
	 *            The file handle or file name to write to
	 * @param startOffset
	 *            The start offset into the file
	 * @param endOffset
	 *            The end offset from the start of the file
	 */
	public FileIO(RandomAccessFile file, int startOffset, int endOffset) {
		this.file = file;
		this.currentOffset = startOffset;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
	}

	@Override
	public void close() throws Exception {
		file.close();
	}

	@Override
	public int size() {
		return endOffset - startOffset;
	}

	@Override
	public FileIO get(int slice) throws IOException {
		if (slice < 0 || slice >= size()) {
			throw new ArrayIndexOutOfBoundsException(slice);
		}
		return new FileIO(file, startOffset + slice, startOffset + slice + 1);
	}

	@Override
	public FileIO get(Slice slice) throws IOException {
		int from = startOffset;
		int to = endOffset;
		if (slice.start != null) {
			if (slice.start < 0) {
				from = endOffset + slice.start;
			} else {
				from += slice.start;
			}
		}
		if (slice.stop != null) {
			if (slice.stop < 0) {
				to = endOffset + slice.stop;
			} else {
				to += slice.stop;
			}
		}
		if (from < startOffset || from > endOffset) {
			throw new ArrayIndexOutOfBoundsException(slice.start);
		}
		if (to < startOffset || to > endOffset) {
			throw new ArrayIndexOutOfBoundsException(slice.stop);
		}
		if (from == to) {
			throw new ArrayIndexOutOfBoundsException(
					"zero-sized regions are not supported");
		}
		return new FileIO(file, from, to);
	}

	@Override
	public boolean isClosed() {
		return !file.getChannel().isOpen();
	}

	@Override
	public void flush() throws IOException {
		// Do nothing
	}

	@Override
	public void seek(int position) throws IOException {
		position += startOffset;
		if (position < startOffset || position > endOffset) {
			throw new IOException("illegal seek position");
		}
		currentOffset = position;
	}

	@Override
	public int tell() throws IOException {
		return currentOffset - startOffset;
	}

	@Override
	public int getAddress() {
		return currentOffset;
	}

	@Override
	public byte[] read(Integer numBytes) throws IOException {
		if (numBytes != null && numBytes == 0) {
			return new byte[0];
		}
		int n = (numBytes == null || numBytes < 0) ? (endOffset - currentOffset)
				: numBytes;
		if (currentOffset + n > endOffset) {
			throw new EOFException();
		}
		synchronized (file) {
			file.seek(currentOffset);
			byte[] data = new byte[n];
			file.readFully(data, 0, n);
			currentOffset += n;
			return data;
		}
	}

	@Override
	public int write(byte[] data) throws IOException {
		int n = data.length;
		if (currentOffset + n > endOffset) {
			throw new EOFException();
		}
		synchronized (file) {
			file.seek(currentOffset);
			file.write(data, 0, n);
		}
		currentOffset += n;
		return n;
	}

	@Override
	public void fill(int value, Integer size, DataType type)
			throws IOException {
		int len = (size == null) ? (endOffset - currentOffset) : size;
		if (currentOffset + len > endOffset) {
			throw new EOFException();
		}
		if (len < 0 || len % type.size != 0) {
			throw new IllegalArgumentException(
					"length to fill must be multiple of fill unit size");
		}
		ByteBuffer b = allocate(len).order(LITTLE_ENDIAN);
		while (b.hasRemaining()) {
			type.writeTo(value, b);
		}
		synchronized (file) {
			file.seek(currentOffset);
			file.write(b.array(), b.arrayOffset(), len);
		}
		currentOffset += len;
	}
}
