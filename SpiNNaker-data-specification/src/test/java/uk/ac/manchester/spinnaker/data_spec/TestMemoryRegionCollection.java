package uk.ac.manchester.spinnaker.data_spec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.data_spec.exceptions.RegionInUseException;

class TestMemoryRegionCollection {

	@Test
	@SuppressWarnings("deprecation")
	void testUnimplemented() {
		MemoryRegionCollection c = new MemoryRegionCollection(1);
		assertThrows(UnsupportedOperationException.class, () -> c.add(null));
		assertThrows(UnsupportedOperationException.class, () -> c.remove(null));
		assertThrows(UnsupportedOperationException.class, () -> c.addAll(null));
		assertThrows(UnsupportedOperationException.class,
				() -> c.removeAll(null));
		assertThrows(UnsupportedOperationException.class,
				() -> c.retainAll(null));
		assertThrows(UnsupportedOperationException.class, () -> c.clear());
	}

	@Test
	void testSingleRegion() throws RegionInUseException {
		MemoryRegionCollection c = new MemoryRegionCollection(1);
		assertEquals(1, c.size());
		assertFalse(c.isEmpty());
		assertTrue(new MemoryRegionCollection(0).isEmpty());
		MemoryRegion mr = new MemoryRegion(123, false, 5);
		c.set(0, mr);
		assertThrows(RegionInUseException.class, () -> c.set(0, mr));
		assertFalse(c.isEmpty(0));
		assertFalse(c.isUnfilled(0));
		assertEquals(5, c.getSize(0));
		assertEquals(1, c.countUsedRegions());
		assertTrue(c.needsToWriteRegion(0));
		assertTrue(c.contains(mr));
		assertTrue(c.containsAll(Collections.singleton(mr)));
		assertArrayEquals(new MemoryRegion[] {
				mr
		}, c.toArray(new MemoryRegion[0]));
		assertArrayEquals(new MemoryRegion[] {
				mr
		}, c.toArray(new MemoryRegion[1]));
		assertArrayEquals(new Object[] {
				mr
		}, c.toArray());
	}

	@Test
	void testMultiRegions() throws RegionInUseException {
		MemoryRegionCollection c = new MemoryRegionCollection(6);
		MemoryRegion mr1 = new MemoryRegion(123, true, 5);
		MemoryRegion mr2 = new MemoryRegion(123, false, 7);
		c.set(2, mr1);
		c.set(4, mr2);
		assertTrue(c.needsToWriteRegion(1));
		assertFalse(c.needsToWriteRegion(5));
		assertEquals(6, c.size());
		c.spliterator();
		assertArrayEquals(new Object[] {
				null, null, mr1, null, mr2, null, null
		}, c.toArray(new Object[7]));
		assertFalse(c.containsAll(
				Arrays.asList(mr1, mr2, new MemoryRegion(123, true, 123))));
		assertThrows(IllegalArgumentException.class,
				() -> c.needsToWriteRegion(6));
	}
}
