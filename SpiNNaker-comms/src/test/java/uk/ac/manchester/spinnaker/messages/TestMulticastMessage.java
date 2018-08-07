package uk.ac.manchester.spinnaker.messages;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestMulticastMessage {
	@Test
	void testCreateWithoutPayload() {
		MulticastMessage msg = new MulticastMessage(1);
		assertEquals(1, msg.key);
		assertNull(msg.payload);
	}

	@Test
	void testCreateWithPayload() {
		MulticastMessage msg = new MulticastMessage(1, 100);
		assertEquals(1, msg.key);
		assertEquals(new Integer(100), msg.payload);
	}
}
