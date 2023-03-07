/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.machine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class TestRegionLocation {

	@Test
	public void testBasicUse() {
		var l1 = new CoreLocation(2, 4, 6);
		var r1 = new RegionLocation(l1, 8);
		assertEquals(2, r1.getX());
		assertEquals(4, r1.getY());
		assertEquals(6, r1.getP());
		assertEquals(8, r1.region);
		assertEquals(r1, r1);
		assertNotEquals(r1, "r1");

		var l2 = new CoreLocation(2, 4, 6);
		var r2 = new RegionLocation(l2, 8);
		assertEquals(r1, r2);
		assertEquals(r1.toString(), r2.toString());
		assertEquals(r1.hashCode(), r2.hashCode());
	}

	private void greater(RegionLocation big, RegionLocation small) {
		assertThat(big, greaterThan(small));
		assertThat(small, lessThan(big));
		assertNotEquals(big, small);
		assertNotEquals(big.hashCode(), small.hashCode());
		assertNotEquals(big.toString(), small.toString());
	}

	@Test
	@SuppressWarnings("SelfComparison")
	public void testCompare() {
		var core001 = new CoreLocation(0, 0, 1);
		var r0012 = new RegionLocation(core001, 2);
		var r0012a = new RegionLocation(core001, 2);
		var core002 = new CoreLocation(0, 0, 2);
		var r0022 = new RegionLocation(core002, 2);
		var core102 = new CoreLocation(1, 0, 2);
		var r1022 = new RegionLocation(core102, 2);
		var core013 = new CoreLocation(0, 1, 3);
		var r0132 = new RegionLocation(core013, 2);
		var core114 = new CoreLocation(1, 1, 4);
		var r1142 = new RegionLocation(core114, 2);
		var r1145 = new RegionLocation(core114, 5);
		greater(r1142, r0012);
		greater(r1142, r0132);
		greater(r1142, r1022);
		greater(r1022, r0132);
		greater(r1142, r1022);
		greater(r0022, r0012);
		greater(r1145, r1142);
		assertEquals(0, r0012.compareTo(r0012));
		assertEquals(0, r0012.compareTo(r0012a));
	}

}
