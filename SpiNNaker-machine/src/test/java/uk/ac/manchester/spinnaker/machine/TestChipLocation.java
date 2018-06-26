package uk.ac.manchester.spinnaker.machine;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestChipLocation {

	@Test
	public void testChipLocationBasicUse() {
		ChipLocation l1 = new ChipLocation(0, 0);
		ChipLocation l2 = new ChipLocation(0, 0);
		assertEquals(0, l2.getX());
		assertEquals(0, l2.getY());
		assertEquals(l1, l2);
		ChipLocation l3 = new ChipLocation(0, 1);
		assertEquals(0, l3.getX());
		assertEquals(1, l3.getY());
		assertNotEquals(l1,l3);
		ChipLocation l4 = new ChipLocation(1, 0);
		assertEquals(1, l4.getX());
		assertEquals(0, l4.getY());
		assertNotEquals(l1,l4);

		Map<ChipLocation, Integer> m = new HashMap<>();
		m.put(l1, 123);
		assertEquals(123, (int) m.get(l2));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testChipLocationRangesXmin() {
		new ChipLocation(-1, 0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testChipLocationRangesYmin() {
		new ChipLocation(0, -1);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testChipLocationRangesXmax() {
		new ChipLocation(257, 0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testChipLocationRangesYmax() {
		new ChipLocation(0, 257);
	}
}
