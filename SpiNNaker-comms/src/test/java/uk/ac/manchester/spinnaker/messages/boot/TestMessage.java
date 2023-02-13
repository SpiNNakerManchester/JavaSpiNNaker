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
package uk.ac.manchester.spinnaker.messages.boot;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.MachineVersion.FIVE;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class TestMessage {

	@Test
	void testBootMessage() {
		var msg = new BootMessage(BootOpCode.HELLO, 0, 0, 0);

		assertNull(msg.data);
		assertEquals(BootOpCode.HELLO, msg.opcode);
		assertEquals(0, msg.operand1);
		assertEquals(0, msg.operand2);
		assertEquals(0, msg.operand3);

		var b = ByteBuffer.allocate(2048);
		msg.addToBuffer(b);
		b.flip();
		var msg2 = new BootMessage(b);

		assertNull(msg2.data);
		assertEquals(BootOpCode.HELLO, msg2.opcode);
		assertEquals(0, msg2.operand1);
		assertEquals(0, msg2.operand2);
		assertEquals(0, msg2.operand3);
	}

	private static final int BOOT_STRUCT_REPLACE_OFFSET = 384;

	@Test
	void testBootMessages() {
		var bm = new BootMessages(FIVE);
		var bml = bm.getMessages().collect(Collectors.toList());
		assertEquals(30, bml.size());
		var patched = bml.get(1).data;
		patched.position(BOOT_STRUCT_REPLACE_OFFSET);
		var got = new byte[16];
		patched.get(got);
		byte[] expected = {
			0, 0, 0, 0, // 0-3
			0, 0, 0, 0, // 4-7
			0, 5, 0, 0, // 8-11
			0, 51, 4, 4 // 12-15
		};
		assertArrayEquals(expected, got);
	}

	private static final List<Integer> EXPECTED_SIZES = List.of(18, 1042, 690);

	@Test
	void testBootMessagesSerialize() {
		var bm = new BootMessages(FIVE);
		for (var b : bm.getMessages().collect(Collectors.toList())) {
			var buf = ByteBuffer.allocate(1500);
			b.addToBuffer(buf);
			buf.flip();
			assertTrue(EXPECTED_SIZES.contains(buf.remaining()),
					() -> format("%d not in %s", buf.remaining(),
							EXPECTED_SIZES));
		}
	}
}
