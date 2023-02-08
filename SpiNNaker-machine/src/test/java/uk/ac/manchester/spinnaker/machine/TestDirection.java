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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian-B
 */
public class TestDirection {

	public TestDirection() {
	}

	/**
	 * Test of values method, of class Direction.
	 */
	@Test
	public void testValues() {
		var result = Direction.values();
		assertEquals(6, result.length);
	}

	/**
	 * Test of valueOf method, of class Direction.
	 */
	@Test
	public void testValueOf() {
		var name = "NORTH";
		var expResult = Direction.NORTH;
		var result = Direction.valueOf(name);
		assertEquals(expResult, result);
	}

	/**
	 * Test of byId method, of class Direction.
	 */
	@Test
	public void testById() {
		int id = 2;
		var expResult = Direction.NORTH;
		var result = Direction.byId(id);
		assertEquals(expResult, result);
	}

	/**
	 * Test the inverse and that they all return unique values.
	 *
	 * This makes sure two values do not use the same default.
	 */
	@Test
	public void testInverse() {
		var inverses = stream(Direction.values()).map(Direction::inverse)
				.collect(toSet());
		assertEquals(Direction.values().length, inverses.size());
	}

	/**
	 * Test that json labels can be converted to direction.
	 */
	@Test
	public void testByLabel() {
		assertEquals(Direction.NORTHEAST, Direction.byLabel("north_east"));
		assertEquals(Direction.WEST, Direction.byLabel("west"));
		assertThrows(Exception.class, () -> {
			Direction.byLabel("foo");
		});
	}
}
