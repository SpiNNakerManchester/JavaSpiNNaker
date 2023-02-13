/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.data_spec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class TestMemoryRegionCollection {

	@Test
	@SuppressWarnings({"deprecation", "DoNotCall"})
	void testUnimplemented() {
		var c = new MemoryRegionCollection(1);
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
		var c = new MemoryRegionCollection(1);
		assertEquals(1, c.size());
		assertFalse(c.isEmpty());
		assertTrue(new MemoryRegionCollection(0).isEmpty());
		var mr = new MemoryRegionReal(0, false, 5);
		c.set(mr);
		assertThrows(RegionInUseException.class, () -> c.set(mr));
		assertFalse(c.isEmpty(0));
		assertFalse(c.isUnfilled(0));
		assertEquals(5, c.getSize(0));
		assertEquals(1, c.countUsedRegions());
		assertTrue(c.needsToWriteRegion(0));
		assertTrue(c.contains(mr));
		assertTrue(c.containsAll(Set.of(mr)));
		assertArrayEquals(new MemoryRegionReal[] {
			mr
		}, c.toArray(new MemoryRegionReal[0]));
		assertArrayEquals(new MemoryRegionReal[] {
			mr
		}, c.toArray(new MemoryRegionReal[1]));
		assertArrayEquals(new Object[] {
			mr
		}, c.toArray());
	}

	@Test
	void testMultiRegions() throws RegionInUseException {
		var c = new MemoryRegionCollection(6);
		var mr1 = new MemoryRegionReal(2, true, 5);
		var mr2 = new MemoryRegionReal(4, false, 7);
		c.set(mr1);
		c.set(mr2);
		assertTrue(c.needsToWriteRegion(1));
		assertFalse(c.needsToWriteRegion(5));
		assertEquals(6, c.size());
		assertNotNull(c.spliterator());
		assertArrayEquals(new Object[] {
			null, null, mr1, null, mr2, null, null
		}, c.toArray(new Object[7]));
		assertFalse(c.containsAll(
				List.of(mr1, mr2, new MemoryRegionReal(5, true, 123))));
		assertThrows(IllegalArgumentException.class,
				() -> c.needsToWriteRegion(6));
	}
}
