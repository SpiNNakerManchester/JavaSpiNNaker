package uk.ac.manchester.spinnaker.messages.model;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.messages.model.ChipInfo;

class TestChipInfo {

	@Test
	void testCreateWithBlankBuffer() {
		ByteBuffer b = ByteBuffer.allocate(256);
		ChipInfo c = new ChipInfo(b);
		assertEquals(0, c.getSDRAMClock());
	}

}
