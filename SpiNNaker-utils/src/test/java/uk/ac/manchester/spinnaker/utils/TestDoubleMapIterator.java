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

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestDoubleMapIterator {

	@Test
	public void testSingle() {
		var aMap = new HashMap<Double, Map<String, Integer>>();

		var inner = new HashMap<String, Integer>();
		inner.put("One", 1);
		inner.put("Two", 2);
		inner.put("Three", 3);
		aMap.put(23.2, inner);

		var instance = new DoubleMapIterator<>(aMap);
		int count = 0;
		while (instance.hasNext()) {
			int value = instance.next();
			assertEquals(value, value); // TODO real test
			count += 1;
		}
		assertEquals(3, count);
	}

	@Test
	public void testMultiple() {
		var aMap = new HashMap<Double, Map<String, Integer>>();

		var inner = new HashMap<String, Integer>();
		inner.put("One", 1);
		inner.put("Two", 2);
		inner.put("Three", 3);
		aMap.put(23.2, inner);

		var inner2 = new HashMap<String, Integer>();
		inner2.put("Ten", 10);
		inner2.put("Eleven", 11);
		inner2.put("Twelve", 12);
		aMap.put(43.6, inner2);

		var instance = new DoubleMapIterator<>(aMap);
		int count = 0;
		while (instance.hasNext()) {
			int value = instance.next();
			assertEquals(value, value); // TODO real test
			count += 1;
		}
		assertEquals(6, count);

		assertThrows(NoSuchElementException.class, () -> {
			instance.next();
		});

	}

	@Test
	public void testEmptyWhole() {
		var aMap = new HashMap<Double, Map<String, Integer>>();
		var instance = new DoubleMapIterator<>(aMap);
		int count = 0;
		while (instance.hasNext()) {
			int value = instance.next();
			assertEquals(value, value); // TODO real test
			// System.out.println(value);
			count += 1;
		}
		assertEquals(0, count);
	}

	@Test
	public void testOneEmpty() {
		var aMap = new HashMap<Double, Map<String, Integer>>();

		var inner0 = new HashMap<String, Integer>();
		aMap.put(343.2, inner0);

		var inner = new HashMap<String, Integer>();
		inner.put("One", 1);
		inner.put("Two", 2);
		inner.put("Three", 3);
		aMap.put(23.2, inner);

		var inner2 = new HashMap<String, Integer>();
		inner2.put("Ten", 10);
		inner2.put("Eleven", 11);
		inner2.put("Twelve", 12);
		aMap.put(43.6, inner2);

		var instance = new DoubleMapIterator<>(aMap);
		int count = 0;
		while (instance.hasNext()) {
			int value = instance.next();
			assertEquals(value, value); // TODO real test
			count += 1;
		}
		assertEquals(6, count);
	}

}
