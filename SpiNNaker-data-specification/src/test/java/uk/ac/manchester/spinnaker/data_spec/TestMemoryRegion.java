package uk.ac.manchester.spinnaker.data_spec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestMemoryRegion {

	@Test
	void test() {
		MemoryRegion r = new MemoryRegion(123, false, 5);
		assertEquals(123, r.getMemoryPointer());
		assertEquals(5, r.getAllocatedSize());
		assertFalse(r.isUnfilled());
		assertEquals(5, r.getRemainingSpace());
		assertEquals(0, r.getWritePointer());
		r.writeIntoRegionData(new byte[] {1, 2, 3, 4});
		assertEquals(123, r.getMemoryPointer());
		assertEquals(5, r.getAllocatedSize());
		assertFalse(r.isUnfilled());
		assertEquals(1, r.getRemainingSpace());
		assertEquals(4, r.getWritePointer());
	}

}
