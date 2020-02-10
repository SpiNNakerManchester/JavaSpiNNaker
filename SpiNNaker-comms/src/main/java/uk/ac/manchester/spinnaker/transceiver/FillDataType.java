/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.transceiver;

import java.nio.ByteBuffer;

/**
 * The fill unit for a fill of values in memory.
 */
public enum FillDataType {
	/** Fill by words (4 bytes). */
	WORD(4),
	/** Fill by half words (2 bytes). */
	HALF_WORD(2),
	/** Fill by single bytes. */
	BYTE(1);
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
	public void writeTo(int value, ByteBuffer buffer) {
		switch (this) {
		case WORD:
			buffer.putInt(value);
			break;
		case HALF_WORD:
			buffer.putShort((short) value);
			break;
		case BYTE:
			buffer.put((byte) value);
			break;
		default:
			// unreachable
			// CHECKSTYLE:OFF
			throw new IllegalStateException();
			// CHECKSTYLE:ON
		}
	}
}
