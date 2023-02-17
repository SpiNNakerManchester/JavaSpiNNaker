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

import java.nio.ByteBuffer;

/**
 * The fill unit for a fill of values in memory.
 */
public enum FillDataType {
	/** Fill by words (4 bytes). */
	WORD(4) {
		@Override
		public void writeTo(int value, ByteBuffer buffer) {
			buffer.putInt(value);
		}
	},
	/** Fill by half words (2 bytes). */
	HALF_WORD(2) {
		@Override
		public void writeTo(int value, ByteBuffer buffer) {
			buffer.putShort((short) value);
		}
	},
	/** Fill by single bytes. */
	BYTE(1) {
		@Override
		public void writeTo(int value, ByteBuffer buffer) {
			buffer.put((byte) value);
		}
	};

	/** The encoding of the fill unit size. */
	public final int size;

	FillDataType(int value) {
		this.size = value;
	}

	/**
	 * Write a value to the buffer in an appropriate way for this fill unit.
	 *
	 * @param value
	 *            The value to write.
	 * @param buffer
	 *            The buffer to write to.
	 */
	public abstract void writeTo(int value, ByteBuffer buffer);
}
