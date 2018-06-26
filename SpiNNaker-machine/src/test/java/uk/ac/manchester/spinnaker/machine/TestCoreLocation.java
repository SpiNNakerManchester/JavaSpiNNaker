package uk.ac.manchester.spinnaker.machine;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestCoreLocation {

	@Test
	public void testChipLocationBasicUse() {
		CoreLocation l1 = new CoreLocation(0, 0, 0);
		CoreLocation l2 = new CoreLocation(0, 0, 0);
		assertEquals(0, l2.getX());
		assertEquals(0, l2.getY());
		assertEquals(0, l2.getP());
		assertEquals(l1, l2);
		assertTrue(l2.onSameCoreAs(l1));
		CoreLocation l3 = new CoreLocation(0, 1, 0);
		assertEquals(0, l3.getX());
		assertEquals(1, l3.getY());
		assertEquals(0, l3.getP());
		assertNotEquals(l1, l3);
		CoreLocation l4 = new CoreLocation(1, 0, 0);
		assertEquals(1, l4.getX());
		assertEquals(0, l4.getY());
		assertEquals(0, l4.getP());
		assertNotEquals(l1, l4);
		CoreLocation l5 = new CoreLocation(0, 0, 1);
		assertEquals(0, l5.getX());
		assertEquals(0, l5.getY());
		assertEquals(1, l5.getP());
		assertNotEquals(l1, l5);
		assertTrue(l1.onSameChipAs(l5));
		assertFalse(l3.onSameChipAs(l4));

		Map<CoreLocation, Integer> m = new HashMap<>();
		m.put(l1, 123);
		assertEquals(123, (int) m.get(l2));

	}

	@Test(expected = IllegalArgumentException.class)
	public void testChipLocationRangesXmin() {
		new CoreLocation(-1, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testChipLocationRangesYmin() {
		new CoreLocation(0, -1, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testChipLocationRangesPmin() {
		new CoreLocation(0, 0, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testChipLocationRangesXmax() {
		new CoreLocation(257, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testChipLocationRangesYmax() {
		new CoreLocation(0, 257, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testChipLocationRangesPmax() {
		new CoreLocation(0, 0, 18);
	}
}
