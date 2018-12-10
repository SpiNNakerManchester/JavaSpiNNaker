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

import static java.lang.Math.max;
import static java.lang.System.arraycopy;
import static uk.ac.manchester.spinnaker.messages.Constants.BYTE_MASK;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.processes.FillProcess;
import uk.ac.manchester.spinnaker.utils.Slice;

/**
 * Presents a view of an entity (SpiNNaker memory or a file on disk) as
 * something like an IO-capable device.
 *
 * @author Donal Fellows
 */
public interface AbstractIO extends AutoCloseable {
	/** @return The size of the entire region of memory. */
	int size();

	/**
	 * Get a sub-region of this memory object. The index must be in range of the
	 * current region to be valid.
	 *
	 * @param slice
	 *            A single index for a single byte of memory.
	 * @return The slice view of the byte.
	 * @throws ProcessException
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If something goes wrong
	 */
	AbstractIO get(int slice) throws IOException, ProcessException;

	/**
	 * Get a sub-region of this memory object. The slice must be in range of the
	 * current region to be valid.
	 *
	 * @param slice
	 *            A description of a contiguous slice of memory.
	 * @return The slice view of the chunk.
	 * @throws ProcessException
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If something goes wrong
	 */
	AbstractIO get(Slice slice) throws IOException, ProcessException;

	/** @return whether the object has been closed. */
	boolean isClosed();

	/**
	 * Flush any outstanding written data.
	 *
	 * @throws ProcessException
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If something goes wrong
	 */
	void flush() throws IOException, ProcessException;

	/**
	 * Seek to a position within the region.
	 *
	 * @param numBytes
	 *            Where to seek to relative to whence. (Note that this isn't the
	 *            same concept as the address property; this still allows for
	 *            the file region to be restricted by slice.)
	 * @param whence
	 *            Where the numBytes should be measured relative to.
	 * @throws ProcessException
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If something goes wrong
	 */
	default void seek(int numBytes, Seek whence)
			throws IOException, ProcessException {
		switch (whence) {
		case SET:
			seek(numBytes);
			break;
		case CUR:
			seek(numBytes + tell());
			break;
		case END:
			seek(numBytes + size());
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Seek to a position within the region.
	 *
	 * @param numBytes
	 *            The absolute location within the region. (Note that this isn't
	 *            the same concept as the address property; this still allows
	 *            for the file region to be restricted by slice.)
	 * @throws ProcessException
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If something goes wrong
	 */
	void seek(int numBytes) throws IOException, ProcessException;

	/**
	 * Return the current position within the region relative to the start.
	 *
	 * @return the current offset
	 * @throws ProcessException
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If something goes wrong
	 */
	int tell() throws IOException, ProcessException;

	/** @return the current absolute address within the region. */
	int getAddress();

	/**
	 * Read the rest of the data.
	 *
	 * @return The bytes that have been read.
	 * @throws ProcessException
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If the read will be beyond the end of the region
	 */
	default byte[] read() throws IOException, ProcessException {
		return read(null);
	}

	/**
	 * Read a number of bytes, or the rest of the data if numBytes is null or
	 * negative.
	 *
	 * @param numBytes
	 *            The number of bytes to read
	 * @return The bytes that have been read.
	 * @throws ProcessException
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If the read will be beyond the end of the region
	 */
	byte[] read(Integer numBytes) throws IOException, ProcessException;

	/**
	 * Write some data to the region.
	 *
	 * @param data
	 *            The data to write
	 * @return The number of bytes written
	 * @throws ProcessException
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If the write will go over the end of the region
	 */
	int write(byte[] data) throws IOException, ProcessException;

	/**
	 * Fill the rest of the region with repeated data words.
	 *
	 * @param value
	 *            The value to repeat
	 * @throws ProcessException
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If the amount of data to fill is more than the region
	 */
	default void fill(int value) throws IOException, ProcessException {
		fill(value, null, FillProcess.DataType.WORD);
	}

	/**
	 * Fill the next part of the region with repeated data words.
	 *
	 * @param value
	 *            The value to repeat
	 * @param size
	 *            Number of bytes to fill from current position
	 * @throws ProcessException
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If the amount of data to fill is more than the region
	 */
	default void fill(int value, int size)
			throws IOException, ProcessException {
		fill(value, size, FillProcess.DataType.WORD);
	}

	/**
	 * Fill the rest of the region with repeated data.
	 *
	 * @param value
	 *            The value to repeat
	 * @param type
	 *            The type of the repeat value
	 * @throws ProcessException
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If the amount of data to fill is more than the region
	 */
	default void fill(int value, FillProcess.DataType type)
			throws IOException, ProcessException {
		fill(value, null, type);
	}

	/**
	 * Fill the next part of the region with repeated data.
	 *
	 * @param value
	 *            The value to repeat
	 * @param size
	 *            Number of bytes to fill from current position, or {@code null}
	 *            to fill to the end
	 * @param type
	 *            The type of the repeat value
	 * @throws ProcessException
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If the amount of data to fill is more than the region
	 */
	void fill(int value, Integer size, FillProcess.DataType type)
			throws IOException, ProcessException;

	/**
	 * The various positions that a {@link #seek(int,Seek)} may be relative to.
	 */
	enum Seek {
		/** Seek relative to the start of the region. */
		SET,
		/** Seek relative to the current location in the region. */
		CUR,
		/** Seek relative to the end of the region. */
		END
	}

	/**
	 * Get a view of this IO object as a Java input stream.
	 *
	 * @return The input stream.
	 */
	default InputStream asInputStream() {
		return new InputStream() {
			@Override
			public int read() throws IOException {
				try {
					byte[] b = AbstractIO.this.read(1);
					return b[0];
				} catch (EOFException e) {
					return -1;
				} catch (ProcessException e) {
					throw new IOException(e);
				}
			}

			@Override
			public int read(byte[] buffer) throws IOException {
				try {
					byte[] b = AbstractIO.this.read(buffer.length);
					arraycopy(b, 0, buffer, 0, b.length);
					return b.length;
				} catch (EOFException e) {
					return -1;
				} catch (ProcessException e) {
					throw new IOException(e);
				}
			}

			@Override
			public int read(byte[] buffer, int offset, int length)
					throws IOException {
				try {
					byte[] b = AbstractIO.this.read(length);
					arraycopy(b, 0, buffer, offset, b.length);
					return b.length;
				} catch (EOFException e) {
					return -1;
				} catch (ProcessException e) {
					throw new IOException(e);
				}
			}

			@Override
			public long skip(long n) throws IOException {
				try {
					int before = tell();
					seek(max((int) n, 0), Seek.CUR);
					int after = tell();
					return after - before;
				} catch (ProcessException e) {
					throw new IOException(e);
				}
			}
		};
	}

	/**
	 * Get a view of this IO object as a Java input stream.
	 *
	 * @return The input stream.
	 */
	default OutputStream asOutputStream() {
		return new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				byte[] buffer = new byte[1];
				buffer[0] = (byte) (b & BYTE_MASK);
				try {
					AbstractIO.this.write(buffer);
				} catch (ProcessException e) {
					throw new IOException(e);
				}
			}

			@Override
			public void write(byte[] bytes) throws IOException {
				try {
					AbstractIO.this.write(bytes);
				} catch (ProcessException e) {
					throw new IOException(e);
				}
			}

			@Override
			public void write(byte[] bytes, int offset, int length)
					throws IOException {
				byte[] buffer = new byte[length];
				arraycopy(bytes, offset, buffer, 0, length);
				try {
					AbstractIO.this.write(buffer);
				} catch (ProcessException e) {
					throw new IOException(e);
				}
			}

			@Override
			public void flush() throws IOException {
				try {
					AbstractIO.this.flush();
				} catch (ProcessException e) {
					throw new IOException(e);
				}
			}
		};
	}
}
