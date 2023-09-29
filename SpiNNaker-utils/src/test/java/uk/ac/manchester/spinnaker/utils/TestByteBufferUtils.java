/*
 * Copyright (c) 2018 The University of Manchester
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

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.BufferOverflowException;
import java.nio.ReadOnlyBufferException;

import org.junit.jupiter.api.Test;

/**
 *
 * @author Donal
 */
public class TestByteBufferUtils {
	@Test
	public void testSlice() {
		var bb = ByteBufferUtils.alloc(12);
		var s = bb.slice(4, 4).order(LITTLE_ENDIAN);
		s.putInt(0x01020304);

		// Check range enforcement
		assertThrows(BufferOverflowException.class, () -> s.put((byte) 0));

		// Check write-through worked
		assertEquals(0, bb.getInt());
		assertEquals(0x01020304, bb.getInt());
		assertEquals(0, bb.getInt());

		// Check read-only carried around
		bb = ByteBufferUtils.readOnly(bb);
		var r = bb.slice(4, 4).order(LITTLE_ENDIAN);
		assertThrows(ReadOnlyBufferException.class, () -> r.putInt(0x5060708));
		assertEquals(0x01020304, r.getInt());

		// Check range sanity enforced
		assertThrows(IndexOutOfBoundsException.class,
				() -> r.slice(0, 16));
	}
}
