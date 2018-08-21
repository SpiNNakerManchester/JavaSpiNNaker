package uk.ac.manchester.spinnaker.messages.model;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.CoreLocation;

class TestCpuInfo {

	@Test
	void testCreateWithBlankBuffer() {
		ByteBuffer b = ByteBuffer.allocate(256);
		CPUInfo c = new CPUInfo(new CoreLocation(0,0,0),b);
		assertEquals(0, c.getApplicationMailboxDataAddress());
	}

}
