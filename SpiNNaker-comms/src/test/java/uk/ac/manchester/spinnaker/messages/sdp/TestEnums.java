package uk.ac.manchester.spinnaker.messages.sdp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestEnums {

	@Test
	void testSdpHeaderFlag() {
		assertEquals(0x7, SDPHeader.Flag.REPLY_NOT_EXPECTED.value);
		assertEquals((byte) 0x87, SDPHeader.Flag.REPLY_EXPECTED.value);
	}

	@Test
	void testSdpPort() {
		assertEquals(0, SDPPort.DEFAULT_PORT.value);
		assertEquals(3, SDPPort.RUNNING_COMMAND_SDP_PORT.value);
	}
}
