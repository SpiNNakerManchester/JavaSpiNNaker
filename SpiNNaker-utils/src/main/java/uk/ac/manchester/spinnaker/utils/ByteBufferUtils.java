/*
 * Copyright (c) 2022 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

	private static final int WORD_SIZE = 4;

	/**
	 * Allocate a new little-endian byte buffer.
	 *
	 * @param capacity
	 *            The capacity of the buffer.
	 * @return The buffer.
	 */
	public static ByteBuffer alloc(int capacity) {
		return ByteBuffer.allocate(capacity).order(LITTLE_ENDIAN);
	}

	/**
	 * Convert a word to a buffer that could form part of a message understood
	 * by SpiNNaker.
	 *
	 * @param value
	 *            The value to put in the buffer as a single 32-bit word.
	 * @return The buffer, flipped. The buffer is writable and has a backing
	 *         array.
	 */
	public static ByteBuffer wordAsBuffer(int value) {
		var b = alloc(WORD_SIZE);
		b.putInt(value).flip();
		return b;
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
		var b = src.duplicate();
		return () -> new Iterator<>() {
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

	/**
	 * Convert the remaining bytes in a buffer into a read-only buffer.
	 *
	 * @param buffer
	 *            message buffer to convert
	 * @return The read-only view.
	 */
	public static ByteBuffer readOnly(ByteBuffer buffer) {
		return buffer.asReadOnlyBuffer().order(LITTLE_ENDIAN);
	}
}
