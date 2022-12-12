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
package uk.ac.manchester.spinnaker.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;

import org.junit.jupiter.api.Test;

/**
 *
 * @author Donal
 */
public class TestByteBufferUtils {
	@Test
	@SuppressWarnings("removal")
	public void testSlice() {
		var bb = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
		var s = ByteBufferUtils.slice(bb, 4, 4);
		s.putInt(0x01020304);

		// Check range enforcement
		assertThrows(BufferOverflowException.class, () -> s.put((byte) 0));

		// Check write-through worked
		assertEquals(0, bb.getInt());
		assertEquals(0x01020304, bb.getInt());
		assertEquals(0, bb.getInt());

		// Check read-only carried around
		bb = bb.asReadOnlyBuffer();
		var r = ByteBufferUtils.slice(bb, 4, 4);
		assertThrows(ReadOnlyBufferException.class, () -> r.putInt(0x5060708));
		assertEquals(0x01020304, r.getInt());

		// Check range sanity enforced
		assertThrows(IndexOutOfBoundsException.class,
				() -> ByteBufferUtils.slice(r, 0, 16));
	}
}
