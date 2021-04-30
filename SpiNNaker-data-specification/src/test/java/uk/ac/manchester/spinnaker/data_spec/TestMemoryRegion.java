/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.data_spec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestMemoryRegion {

	@Test
	void test() {
		MemoryRegionReal r = new MemoryRegionReal(77, 123, false, 5);
		assertEquals(77, r.getIndex());
		assertEquals(123, r.getMemoryPointer());
		assertEquals(5, r.getAllocatedSize());
		assertFalse(r.isUnfilled());
		assertEquals(5, r.getRemainingSpace());
		assertEquals(0, r.getWritePointer());
		r.writeIntoRegionData(new byte[] {
			1, 2, 3, 4
		});
		assertEquals(77, r.getIndex());
		assertEquals(123, r.getMemoryPointer());
		assertEquals(5, r.getAllocatedSize());
		assertFalse(r.isUnfilled());
		assertEquals(1, r.getRemainingSpace());
		assertEquals(4, r.getWritePointer());
	}
}
