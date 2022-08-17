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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
@SuppressWarnings("unused")
public class TestTripleMapIterator {

	@Test
	public void testMultiple() {
		Map<Float, Map<Double, Map<String, Integer>>> bigMap = new HashMap<>();

		Map<Double, Map<String, Integer>> aMap = new HashMap<>();

		Map<String, Integer> inner = new HashMap<>();
		inner.put("One", 1);
		inner.put("Two", 2);
		inner.put("Three", 3);
		aMap.put(23.2, inner);

		Map<String, Integer> inner2 = new HashMap<>();
		inner2.put("Ten", 10);
		inner2.put("Eleven", 11);
		inner2.put("Twelve", 12);
		aMap.put(43.6, inner2);

		bigMap.put((float) 0.5, aMap);

		Map<Double, Map<String, Integer>> aMap2 = new HashMap<>();

		Map<String, Integer> inner11 = new HashMap<>();
		inner.put("Un", -1);
		inner.put("Duex", -2);
		inner.put("Trois", -3);
		aMap2.put(423.2, inner11);

		Map<String, Integer> inner12 = new HashMap<>();
		inner2.put("Dix", -10);
		inner2.put("Onze", -11);
		inner2.put("Douze", -12);
		aMap2.put(4.6, inner12);

		bigMap.put((float) 2.5, aMap2);

		TripleMapIterator<Integer> instance;
		instance = new TripleMapIterator<>(bigMap);
		int count = 0;
		while (instance.hasNext()) {
			Integer value = instance.next();
			count += 1;
		}
		assertEquals(12, count);
		assertThrows(NoSuchElementException.class, () -> {
			instance.next();
		});

	}

	@Test
	public void testList() {
		ArrayList<Map<Double, Map<String, Integer>>> aList = new ArrayList<>();

		Map<Double, Map<String, Integer>> aMap = new HashMap<>();

		Map<String, Integer> inner = new HashMap<>();
		inner.put("One", 1);
		inner.put("Two", 2);
		inner.put("Three", 3);
		aMap.put(23.2, inner);

		Map<String, Integer> inner2 = new HashMap<>();
		inner2.put("Ten", 10);
		inner2.put("Eleven", 11);
		inner2.put("Twelve", 12);
		aMap.put(43.6, inner2);

		aList.add(aMap);

		Map<Double, Map<String, Integer>> aMap2 = new HashMap<>();

		Map<String, Integer> inner11 = new HashMap<>();
		inner.put("Un", -1);
		inner.put("Duex", -2);
		inner.put("Trois", -3);
		aMap2.put(423.2, inner11);

		Map<String, Integer> inner12 = new HashMap<>();
		inner2.put("Dix", -10);
		inner2.put("Onze", -11);
		inner2.put("Douze", -12);
		aMap2.put(4.6, inner12);

		aList.add(aMap2);

		TripleMapIterator<Integer> instance;
		instance = new TripleMapIterator<>(aList);
		int count = 0;
		while (instance.hasNext()) {
			Integer value = instance.next();
			count += 1;
		}
		assertEquals(12, count);
		assertThrows(NoSuchElementException.class, () -> {
			instance.next();
		});

	}

	@Test
	public void testEmpty() {
		Map<Float, Map<Double, Map<String, Integer>>> bigMap = new HashMap<>();

		TripleMapIterator<Integer> instance;
		instance = new TripleMapIterator<>(bigMap);
		int count = 0;
		while (instance.hasNext()) {
			Integer value = instance.next();
			count += 1;
		}
		assertEquals(0, count);
	}
}
