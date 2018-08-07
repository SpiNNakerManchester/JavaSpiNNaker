package uk.ac.manchester.spinnaker.messages.boot;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

class TestMessage {

	@Test
	void testBootMessage() {
		BootMessage msg = new BootMessage(BootOpCode.HELLO, 0, 0, 0);

		assertNull(msg.data);
		assertEquals(BootOpCode.HELLO, msg.opcode);
		assertEquals(0, msg.operand1);
		assertEquals(0, msg.operand2);
		assertEquals(0, msg.operand3);

		ByteBuffer b = ByteBuffer.allocate(2048);
		msg.addToBuffer(b);
		b.flip();
		BootMessage msg2 = new BootMessage(b);

		assertNull(msg2.data);
		assertEquals(BootOpCode.HELLO, msg2.opcode);
		assertEquals(0, msg2.operand1);
		assertEquals(0, msg2.operand2);
		assertEquals(0, msg2.operand3);
	}

}
