package uk.ac.manchester.spinnaker.machine;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


public class TestChipLocation {

	@Test
	public void testChipLocationBasicUse() {
		ChipLocation l1 = new ChipLocation(0, 0);
		ChipLocation l2 = new ChipLocation(0, 0);
		assertEquals(0, l2.getX());
		assertEquals(0, l2.getY());
		assertEquals(l1, l2);
		assertEquals(l1.hashCode(), l2.hashCode());
		ChipLocation l3 = new ChipLocation(0, 1);
		assertEquals(0, l3.getX());
		assertEquals(1, l3.getY());
		assertNotEquals(l1,l3);
		assertNotEquals(l1.hashCode(),l3.hashCode());
		assertNotEquals(l1, "hello");
		ChipLocation l4 = new ChipLocation(1, 0);
		assertEquals(1, l4.getX());
		assertEquals(0, l4.getY());
		assertNotEquals(l1,l4);
		assertNotEquals(l1.hashCode(),l4.hashCode());

		Map<ChipLocation, Integer> m = new HashMap<>();
		m.put(l1, 123);
		assertEquals(123, (int) m.get(l2));
	}

	@Test
	public void testChipLocationRangesXmin() {
            assertThrows(IllegalArgumentException.class, () -> {
		new ChipLocation(-1, 0);
            });
	}

	@Test
	public void testChipLocationRangesYmin() {
           assertThrows(IllegalArgumentException.class, () -> {
		new ChipLocation(0, -1);
           });
	}

	@Test
	public void testChipLocationRangesXmax() {
           assertThrows(IllegalArgumentException.class, () -> {
		new ChipLocation(257, 0);
           });
	}

	@Test
	public void testChipLocationRangesYmax() {
          assertThrows(IllegalArgumentException.class, () -> {
		new ChipLocation(0, 257);
          });
	}
}
