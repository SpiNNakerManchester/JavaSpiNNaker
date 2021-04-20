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

import java.io.EOFException;
import java.io.IOException;

import uk.ac.manchester.spinnaker.transceiver.FillDataType;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.utils.Slice;

/**
 * Common code for memory IO.
 *
 * @author Donal Fellows
 */
abstract class BaseIO implements AbstractIO {
	/** The start address of the region to write to. */
	final int start;
	/** The end of the region to write to. */
	final int end;
	/** The current pointer where read and writes are taking place. */
	int current;

	BaseIO(int start, int end) {
		if (start >= end) {
			throw new IllegalArgumentException(
					"start address must precede end address");
		}
		this.start = start;
		this.end = end;
		this.current = start;
	}

	@Override
	public int size() {
		return end - start;
	}

	@Override
	public void seek(int numBytes) {
		int position = start + numBytes;
		if (position < start || position > end) {
			throw new IllegalArgumentException(
					"Attempt to seek to a position of " + position
							+ " which is outside of the region");
		}
		current = position;
	}

	@Override
	public int tell() {
		return current - start;
	}

	@Override
	public int getAddress() {
		return current;
	}

	/**
	 * Do the actual read.
	 *
	 * @param numBytes
	 *            Number of bytes to read.
	 * @return The bytes.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	abstract byte[] doRead(int numBytes) throws IOException, ProcessException;

	/**
	 * Do the actual write.
	 *
	 * @param bytes
	 *            The array containing the bytes to write.
	 * @param start
	 *            Index into {@code bytes} where data starts.
	 * @param len
	 *            Number of bytes to write.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	abstract void doWrite(byte[] bytes, int start, int len)
			throws IOException, ProcessException;

	/**
	 * Do the actual fill.
	 *
	 * @param value
	 *            The atomic value to fill.
	 * @param type
	 *            The type of value to fill.
	 * @param len
	 *            The length of data to fill.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	abstract void doFill(int value, FillDataType type, int len)
			throws IOException, ProcessException;

	@Override
	public byte[] read(Integer numBytes) throws IOException, ProcessException {
		if (numBytes != null && numBytes == 0) {
			return new byte[0];
		}
		int n = (numBytes == null || numBytes < 0) ? (end - current) : numBytes;
		if (current + n > end) {
			throw new EOFException();
		}
		byte[] data = doRead(n);
		current += n;
		return data;
	}

	@Override
	public int write(byte[] data) throws IOException, ProcessException {
		int n = data.length;
		if (current + n > end) {
			throw new EOFException();
		}
		doWrite(data, 0, n);
		current += n;
		return n;
	}

	@Override
	public void fill(int value, Integer size, FillDataType type)
			throws IOException, ProcessException {
		int len = (size == null) ? (end - current) : size;
		if (current + len > end) {
			throw new EOFException();
		}
		if (len < 0 || len % type.size != 0) {
			throw new IllegalArgumentException(
					"length to fill must be multiple of fill unit size");
		}
		doFill(value, type, len);
		current += len;
	}

	/**
	 * How to make new instances of a class that are slices.
	 *
	 * @param <V>
	 *            The type that is made.
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	interface SliceFactory<V> {
		/**
		 * Make the slice of the object.
		 *
		 * @param from
		 *            Where the slice starts.
		 * @param to
		 *            Where the slice ends. Greater than {@code from}.
		 * @return The sliced object.
		 */
		V call(int from, int to);
	}

	/**
	 * Core of slice application.
	 *
	 * @param <IO>
	 *            The type of slice produced.
	 * @param slice
	 *            The slice to apply.
	 * @param factory
	 *            How to make a new instance of the current class that
	 *            represents the slice.
	 * @return The sliced object.
	 * @throws ArrayIndexOutOfBoundsException
	 *             If the indices are out of the range supported by this IO
	 *             object.
	 */
	final <IO extends AbstractIO> IO get(Slice slice,
			SliceFactory<IO> factory) {
		int from = start;
		int to = end;
		if (slice.start != null) {
			if (slice.start < 0) {
				from = end + slice.start;
			} else {
				from += slice.start;
			}
		}
		if (slice.stop != null) {
			if (slice.stop < 0) {
				to = end + slice.stop;
			} else {
				to += slice.stop;
			}
		}
		/*
		 * When slice.start is null, from is guaranteed to be equal to start and
		 * start is also guaranteed to be no more than end.
		 *
		 * An equivalent argument holds for slice.stop.
		 */
		if (from < start || from > end) {
			throw new ArrayIndexOutOfBoundsException(slice.start);
		}
		if (to < start || to > end) {
			throw new ArrayIndexOutOfBoundsException(slice.stop);
		}
		if (from == to) {
			throw new ArrayIndexOutOfBoundsException(
					"zero-sized regions are not supported");
		}
		return factory.call(from, to);
	}
}
