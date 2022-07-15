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
package uk.ac.manchester.spinnaker.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestCounter {

	public TestCounter() {
	}

	/**
	 * Test of increment and get methods, of class Counter.
	 */
	@Test
	public void testIncrement() {
		var counter = new Counter();
		assertEquals(0, counter.get());
		counter.increment();
		counter.increment();
		assertEquals(2, counter.get());
	}

	@Test
	public void testAdd() {
		var counter = new Counter();
		assertEquals(0, counter.get());
		counter.add(4);
		counter.add(-2);
		assertEquals(2, counter.get());
	}
}
