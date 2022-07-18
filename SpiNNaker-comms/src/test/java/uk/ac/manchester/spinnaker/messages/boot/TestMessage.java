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
package uk.ac.manchester.spinnaker.messages.boot;

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
			assertTrue(EXPECTED_SIZES.contains(buf.remaining()), () -> String
					.format("%d not in %s", buf.remaining(), EXPECTED_SIZES));
		}
	}
}
