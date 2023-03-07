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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian-B
 */
public class TestLink {

	private final ChipLocation chip00 = new ChipLocation(0, 0);

	private final ChipLocation chip01 = new ChipLocation(0, 1);

	private final ChipLocation chip11 = new ChipLocation(1, 1);

	/**
	 * Test equal Links.
	 */
	@Test
	public void testEquals() {
		var link1 = new Link(chip00, Direction.EAST, chip01);
		var link2 = new Link(chip00, Direction.EAST, chip01);
		assertEquals(link1, link2);
		assertEquals(link1.hashCode(), link2.hashCode());
		assertEquals(link1.toString(), link2.toString());
		assertEquals(link1, link1);
		assertNotEquals(link1, "link1");
		assertNotEquals(link1, null);
	}

	private void checkDifferent(Link link1, Link link2) {
		assertNotEquals(link1, link2);
		assertNotEquals(link1.hashCode(), link2.hashCode());
		assertNotEquals(link1.toString(), link2.toString());
	}

	/**
	 * Test of different Links.
	 */
	@Test
	public void testDifferent() {
		var link1 = new Link(chip00, Direction.EAST, chip01);
		var link2 = new Link(chip11, Direction.EAST, chip01);
		checkDifferent(link1, link2);

		link2 = new Link(chip00, Direction.NORTH, chip01);
		checkDifferent(link1, link2);

		link2 = new Link(chip00, Direction.EAST, chip11);
		checkDifferent(link1, link2);
	}

	public void testById() {
		var link1 = new Link(chip00, Direction.NORTH, chip01);
		var link2 = new Link(chip00, 2, chip01);
		assertEquals(link1, link2);
	}

}
