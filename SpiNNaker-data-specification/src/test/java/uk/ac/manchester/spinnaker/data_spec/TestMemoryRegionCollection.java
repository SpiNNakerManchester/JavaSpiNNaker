package uk.ac.manchester.spinnaker.data_spec;

import static org.junit.jupiter.api.Assertions.*;

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
	void test() throws RegionInUseException {
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
}
