package uk.ac.manchester.spinnaker.messages.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.ChipLocation;

class TestIOBufModel {

	@Test
	void test() throws UnsupportedEncodingException {
		byte[] buf = "Everything failed on chip.".getBytes("ASCII");
		IOBuffer b = new IOBuffer(new ChipLocation(1,2).getScampCore(), buf);
		assertEquals(1, b.getX());
		assertEquals(2, b.getY());
		assertEquals(0, b.getP());
		assertEquals("Everything failed on chip.", b.getContentsString());
	}

}
