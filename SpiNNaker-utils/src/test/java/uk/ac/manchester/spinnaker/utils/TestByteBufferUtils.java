/*
 * Copyright (c) 2018-2023 The University of Manchester
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
		assertThrows(IllegalArgumentException.class,
				() -> ByteBufferUtils.slice(r, 0, 16));
	}
}
