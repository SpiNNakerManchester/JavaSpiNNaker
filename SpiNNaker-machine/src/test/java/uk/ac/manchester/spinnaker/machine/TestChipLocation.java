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
package uk.ac.manchester.spinnaker.machine;

import java.util.HashMap;
import java.util.Map;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class TestChipLocation {

	@Test
	public void testChipLocationBasicUse() {
		var l1 = new ChipLocation(0, 0);
		var l2 = new ChipLocation(0, 0);
		assertEquals(0, l2.getX());
		assertEquals(0, l2.getY());
		assertEquals(l1, l2);
		assertEquals(l1.hashCode(), l2.hashCode());
		var l3 = new ChipLocation(0, 1);
		assertEquals(0, l3.getX());
		assertEquals(1, l3.getY());
		assertNotEquals(l1, l3);
		assertNotEquals(l1.hashCode(), l3.hashCode());
		assertNotEquals(l1, "hello");
		var l4 = new ChipLocation(1, 0);
		assertEquals(1, l4.getX());
		assertEquals(0, l4.getY());
		assertNotEquals(l1, l4);
		assertNotEquals(l1.hashCode(), l4.hashCode());

		Map<ChipLocation, Integer> m = new HashMap<>();
		m.put(l1, 123);
		assertEquals(123, (int) m.get(l2));
	}

	@Test
	public void testEquals() {
		var chip00 = new ChipLocation(0, 0);
		var chip10 = new ChipLocation(1, 0);
		var chip01 = new ChipLocation(0, 1);
		var chip11 = new ChipLocation(1, 1);
		assertThat("11 > 00", chip11, greaterThan(chip00));
		assertThat("11 > 01", chip11, greaterThan(chip01));
		assertThat("11 > 10", chip11, greaterThan(chip10));
		assertThat("10 > 11", chip10, lessThan(chip11));
		assertThat("10 < 01", chip10, greaterThan(chip01));
		assertThat("01 > 10", chip01, lessThan(chip10));
		assertThat("10 < 01", chip11, greaterThan(chip10));
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

	private static class MockChipLocation implements HasChipLocation {

		@Override
		public int getX() {
			return 1;
		}

		@Override
		public int getY() {
			return 2;
		}
	}

	@Test
	public void testDefaults() {
		assertEquals(new CoreLocation(1, 2, 0),
				new MockChipLocation().getScampCore());
		assertEquals(new ChipLocation(1, 2),
				new MockChipLocation().asChipLocation());
	}
}
