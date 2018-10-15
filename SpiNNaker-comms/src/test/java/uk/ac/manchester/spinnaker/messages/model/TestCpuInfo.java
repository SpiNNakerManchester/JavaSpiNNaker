package uk.ac.manchester.spinnaker.messages.model;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.CoreLocation;

class TestCpuInfo {

	@Test
	void testCreateWithBlankBuffer() {
		ByteBuffer b = ByteBuffer.allocate(256).order(LITTLE_ENDIAN);
		CPUInfo c = new CPUInfo(new CoreLocation(0, 0, 0), b);
		assertEquals(0, c.getApplicationMailboxDataAddress());
	}

}
