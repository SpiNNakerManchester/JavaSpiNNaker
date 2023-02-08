/*
 * Copyright (c) 2018 The University of Manchester
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

import java.util.List;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ONE_ZERO;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author Christian-B
 */
public class TestCoreSubsets {

	public TestCoreSubsets() {
	}

	/**
	 * Test of basic methods, of class CoreSubset.
	 */
	@Test
	public void testBasic() {
		var instance = new CoreSubsets();
		assertEquals(0, instance.size());
		assertTrue(instance.isEmpty());

		instance.addCore(0, 0, 1);
		assertEquals(1, instance.size());
		assertFalse(instance.isEmpty());

		instance.addCores(0, 0, List.of(1));
		assertEquals(1, instance.size());
		assertFalse(instance.isEmpty());
		assertFalse(instance.isChip(ONE_ZERO));

		instance.addCores(1, 0, List.of(1, 2));
		assertEquals(3, instance.size());
		assertFalse(instance.isEmpty());

		assertTrue(instance.isChip(ONE_ZERO));
		assertTrue(instance.isCore(new CoreLocation(0, 0, 1)));
		assertFalse(instance.isCore(new CoreLocation(2, 0, 1)));
		assertFalse(instance.isChip(new ChipLocation(3, 1)));
		assertFalse(instance.isCore(new CoreLocation(0, 0, 14)));
	}

	@Test
	public void testAdd() {
		var instance = new CoreSubsets();
		instance.addCore(new CoreLocation(0, 0, 1));
		// get hashcode to make subset immutable
		@SuppressWarnings("unused")
		int hash = instance.hashCode();
		assertThrows(IllegalStateException.class, () -> {
			instance.addCore(new CoreLocation(0, 0, 2));
		});
		assertThrows(IllegalStateException.class, () -> {
			instance.addCores(new ChipLocation(0, 0), List.of(1, 2, 3));
		});
		assertThrows(IllegalStateException.class, () -> {
			instance.addCore(new ChipLocation(0, 0), 2);
		});
	}

	public void testMultiple() {
		var locations = List.of(new CoreLocation(0, 0, 1),
				new CoreLocation(0, 0, 2), new CoreLocation(0, 0, 3),
				new CoreLocation(0, 1, 1), new CoreLocation(0, 1, 2),
				new CoreLocation(0, 1, 3), new CoreLocation(0, 0, 1),
				new CoreLocation(0, 0, 2), new CoreLocation(0, 0, 3),
				new CoreLocation(0, 0, 1), new CoreLocation(0, 0, 2),
				new CoreLocation(0, 0, 3), new CoreLocation(0, 0, 4));
		var css = new CoreSubsets(locations);

		var locations2 = List.of(new CoreLocation(0, 0, 4),
				new CoreLocation(0, 0, 5), new CoreLocation(0, 0, 6));
		css.addCores(locations2);

		assertTrue(css.isChip(new ChipLocation(0, 1)));
		assertTrue(css.isCore(new CoreLocation(0, 0, 6)));

		assertTrue(css.isCore(new CoreLocation(0, 1, 3)));

		int count = 0;
		for (var coreLocation : css) {
			count += 1;
			assertEquals(0, coreLocation.getX());
		}
		assertEquals(9, count);

		count = 0;
		for (var coreLocation : css.coreByChip(ZERO_ZERO)) {
			count += 1;
			assertEquals(0, coreLocation.getX());
			assertEquals(0, coreLocation.getY());
		}
		assertEquals(6, count);
	}

	public void testInterest() {
		var css1 = new CoreSubsets();
		css1.addCores(new ChipLocation(0, 0), List.of(1, 2, 3));
		css1.addCores(new ChipLocation(0, 1), List.of(1, 2, 3));
		css1.addCore(new ChipLocation(1, 1), 1);
		css1.addCore(new ChipLocation(2, 2), 1);
		assertEquals(8, css1.size());
		assertFalse(css1.isEmpty());

		var css2 = new CoreSubsets();
		css2.addCores(new ChipLocation(0, 0), List.of(2, 3, 5));
		css2.addCores(new ChipLocation(1, 0), List.of(1, 2, 3));
		css2.addCores(new ChipLocation(1, 1), List.of(9, 7, 1, 5));
		css2.addCore(new ChipLocation(2, 2), 2);
		assertEquals(11, css2.size());
		assertFalse(css2.isEmpty());

		var css3 = css1.intersection(css2);
		assertTrue(css3.isCore(new CoreLocation(0, 0, 2)));
		assertTrue(css3.isCore(new CoreLocation(0, 0, 3)));
		assertTrue(css3.isCore(new CoreLocation(1, 1, 1)));
		assertEquals(3, css3.size());
		assertFalse(css3.isEmpty());
	}

	public void testEquals() {
		var css1 = new CoreSubsets();
		css1.addCores(new ChipLocation(0, 0), List.of(1, 2, 3));
		css1.addCores(new ChipLocation(0, 1), List.of(1, 2, 3));

		var css2 = new CoreSubsets();
		css2.addCores(new ChipLocation(0, 0), List.of(1, 2, 3));
		css2.addCores(new ChipLocation(0, 1), List.of(1, 3));

		assertNotEquals(css1, css2);
		assertNotEquals(css1.toString(), css2.toString());

		css2.addCore(new CoreLocation(0, 1, 2));
		assertEquals(css1, css2);
		assertEquals(css1.toString(), css2.toString());
		assertEquals(css1, css1);

		assertNotEquals(css1, "css1");
		assertNotEquals(css1, null);
	}

	public void testIterator() {
		var css1 = new CoreSubsets();
		css1.addCores(new ChipLocation(0, 0), List.of(1, 2, 3));
		css1.addCores(new ChipLocation(0, 1), List.of(1, 2, 3));
		int count = 0;
		for (var coreLocation : css1) {
			count += 1;
			assertThat("p > 0", coreLocation.getP(), greaterThan(0));
			assertThat("p < 4", coreLocation.getP(), lessThan(4));
		}
		assertEquals(6, count);
	}

	public void testByChip() {
		var css1 = new CoreSubsets();
		css1.addCores(new ChipLocation(0, 0), List.of(1, 2, 3));
		css1.addCores(new ChipLocation(0, 1), List.of(1, 2, 3));
		int count = 0;
		for (var chip : css1.getChips()) {
			for (var core : css1.coreByChip(chip)) {
				count += 1;
				assertEquals(core.getX(), chip.getX());
				assertEquals(core.getX(), chip.getX());
			}
		}
		assertEquals(6, count);
		count = 0;
		for (var chip : css1.getChips()) {
			for (var p : css1.pByChip(chip)) {
				count += 1;
				assertThat("p > 0", p, greaterThan(0));
				assertThat("p < 4", p, lessThan(4));
			}
		}
		assertEquals(6, count);
	}

	public void testBadIterator() {
		var css1 = new CoreSubsets();
		int count = 0;
		for (@SuppressWarnings("unused") var coreLocation : css1) {
			count += 1;
		}
		assertEquals(0, count);

		var empty = css1.coreByChip(ZERO_ZERO);
		assertEquals(0, empty.size());

		var emptyP = css1.pByChip(ZERO_ZERO);
		assertEquals(0, emptyP.size());

		assertThrows(NoSuchElementException.class, () -> {
			css1.iterator().next();
		});
	}

}
