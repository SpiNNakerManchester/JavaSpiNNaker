/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.Math.max;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * A system for accumulating the results of a sequence of memory reads.
 *
 * @param <T>
 *            The type of the result of accumulation.
 * @author Donal Fellows
 */
interface Accumulator<T> {
	/**
	 * Add a received chunk to the accumulator.
	 *
	 * @param position
	 *            Where the data goes.
	 * @param data
	 *            The received data.
	 */
	void add(int position, ByteBuffer data);

	/**
	 * Complete the accumulation.
	 *
	 * @return Whatever the accumulator deems to be the result.
	 * @throws IOException
	 *             If any file I/O fails.
	 */
	T finish() throws IOException;

	/** Accumulate to a memory buffer. */
	class BufferAccumulator implements Accumulator<ByteBuffer> {
		private final ByteBuffer buffer;

		private boolean done = false;

		private int maxpos = 0;

		/**
		 * Create an accumulator using a buffer of the given size.
		 *
		 * @param size
		 *            How many bytes to hold in the buffer.
		 */
		BufferAccumulator(int size) {
			buffer = allocate(size);
		}

		/**
		 * Create an accumulator wrapping a given buffer.
		 *
		 * @param receivingBuffer
		 *            The buffer to receive data into.
		 */
		BufferAccumulator(ByteBuffer receivingBuffer) {
			buffer = receivingBuffer.slice();
		}

		/**
		 * Create an accumulator wrapping a given buffer.
		 *
		 * @param receivingBuffer
		 *            The array to receive data into.
		 */
		BufferAccumulator(byte[] receivingBuffer) {
			buffer = wrap(receivingBuffer);
		}

		@Override
		public void add(int position, ByteBuffer data) {
			if (done) {
				throw new IllegalStateException(
						"writing to fully written buffer");
			}
			var b = buffer.duplicate();
			b.position(position);
			int after = position + data.remaining();
			b.put(data);
			maxpos = max(maxpos, after);
		}

		/** @return The accumulated data. */
		@Override
		public ByteBuffer finish() {
			if (!done) {
				done = true;
				buffer.limit(maxpos);
			}
			return buffer.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		}
	}

	/** Accumulate to a file. */
	class FileAccumulator implements Accumulator<Void> {
		private final RandomAccessFile file;

		private final long initOffset;

		private boolean done = false;

		private IOException exception;

		private long pos;

		FileAccumulator(RandomAccessFile dataStream) throws IOException {
			file = dataStream;
			initOffset = dataStream.getFilePointer();
			pos = initOffset;
		}

		@Override
		public void add(int position, ByteBuffer data) {
			if (done) {
				throw new IllegalStateException(
						"writing to fully written buffer");
			}
			try {
				long newPos = position + initOffset;
				if (newPos != pos) {
					file.seek(newPos);
					pos = newPos;
				}
				pos += data.remaining();
				if (data.hasArray()) {
					file.write(data.array(),
							data.arrayOffset() + data.position(),
							data.remaining());
				} else {
					byte[] buf = new byte[data.remaining()];
					data.get(buf);
					file.write(buf);
				}
			} catch (IOException e) {
				if (exception == null) {
					exception = e;
				}
			}
		}

		/**
		 * @return Nothing; caller should already know the data stream and file.
		 * @throws IOException
		 *             if any of the writes to the file failed.
		 */
		@Override
		public Void finish() throws IOException {
			done = true;
			if (exception != null) {
				throw exception;
			}
			file.seek(initOffset);
			return null;
		}
	}
}
