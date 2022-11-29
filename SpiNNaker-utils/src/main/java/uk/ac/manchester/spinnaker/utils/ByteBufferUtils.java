/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.utils;

import static java.lang.Math.min;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;

/** Utility methods for {@link ByteBuffer}s. */
public abstract class ByteBufferUtils {
	private ByteBufferUtils() {
	}

	/**
	 * Make a slice of a byte buffer without modifying the original buffer.
	 *
	 * @param src
	 *            The originating buffer.
	 * @param from
	 *            The offset into the originating buffer where the slice starts.
	 * @param len
	 *            The length of the slice.
	 * @return The little-endian slice. This will be read-only if and only if
	 *         the original buffer is read-only.
	 * @deprecated Replace with
	 *             {@code src.slice(from, len).order(LITTLE_ENDIAN)}
	 */
	@Deprecated(forRemoval = true)
	public static ByteBuffer slice(ByteBuffer src, int from, int len) {
		return src.slice(from, len).order(LITTLE_ENDIAN);
	}

	/**
	 * Make a slice of a byte buffer if it exceeds a given size.
	 *
	 * @param src
	 *            The originating buffer.
	 * @param maxSize
	 *            The maximum size of the resulting buffer.
	 * @return The original buffer or a little-endian slice. This will be
	 *         read-only if and only if the original buffer is read-only.
	 */
	public static ByteBuffer limitSlice(ByteBuffer src, int maxSize) {
		if (src.remaining() <= maxSize) {
			return src;
		}
		return src.slice().order(LITTLE_ENDIAN).limit(maxSize);
	}

	/**
	 * Slice up a buffer buffer into a sequence of (little-endian) byte buffers
	 * with a maximum size.
	 *
	 * @param src
	 *            The buffer to slice up.
	 * @param chunkSize
	 *            Max size of each chunk. Must be positive.
	 * @return An iterable of little-endian chunks. Only the final chunk will be
	 *         smaller than the requested chunk size.
	 */
	public static MappableIterable<ByteBuffer> sliceUp(ByteBuffer src,
			int chunkSize) {
		return () -> new Iterator<>() {
			final ByteBuffer b = src.duplicate();

			@Override
			public boolean hasNext() {
				return b.hasRemaining();
			}

			@Override
			public ByteBuffer next() {
				var s = b.slice();
				s.limit(min(chunkSize, s.limit()));
				b.position(b.position() + s.limit());
				return s.order(LITTLE_ENDIAN);
			}
		};
	}

	/**
	 * Read a chunk of an input stream into a byte buffer.
	 *
	 * @param data
	 *            Where to read from.
	 * @param workingBuffer
	 *            The buffer to use. The number of bytes remaining in the buffer
	 *            is the maximum number of bytes to read <em>unless</em> the
	 *            maximum bytes remaining is smaller.
	 * @param maxRemaining
	 *            The maximum number of bytes remaining in the input stream;
	 *            bytes after that point won't be read even if they exist.
	 * @return A view on the buffer with the data in it (endianness undefined),
	 *         or {@code null} if EOF or the limit is reached.
	 * @throws IOException
	 *             If reading fails.
	 */
	public static ByteBuffer read(InputStream data, ByteBuffer workingBuffer,
			int maxRemaining) throws IOException {
		var tmp = workingBuffer.duplicate();
		int size = min(tmp.remaining(), maxRemaining);
		if (size <= 0) {
			return null;
		}
		size = data.read(tmp.array(), tmp.arrayOffset(), size);
		if (size <= 0) {
			return null;
		}
		return tmp.limit(size);
	}
}
