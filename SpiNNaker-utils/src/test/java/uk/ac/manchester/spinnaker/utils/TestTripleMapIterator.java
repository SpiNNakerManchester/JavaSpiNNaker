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
import java.util.List;
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
		var bigMap = Map.of(//
				0.5f, Map.of(//
						23.2, Map.of("One", 1, "Two", 2, "Three", 3), //
						43.6, Map.of("Ten", 10, "Eleven", 11, "Twelve", 12)),
				2.5f, Map.of(//
						423.2, Map.of("Un", -1, "Duex", -2, "Trois", -3), //
						4.6, Map.of("Dix", -10, "Onze", -11, "Douze", -12)));

		var instance = new TripleMapIterator<>(bigMap);
		int count = 0;
		while (instance.hasNext()) {
			var value = instance.next();
			count += 1;
		}
		assertEquals(12, count);
		assertThrows(NoSuchElementException.class, () -> {
			instance.next();
		});

	}

	@Test
	public void testList() {
		var aList = List.of(//
				Map.of(//
						23.2, Map.of("One", 1, "Two", 2, "Three", 3), //
						43.6, Map.of("Ten", 10, "Eleven", 11, "Twelve", 12)), //
				Map.of(//
						423.2, Map.of("Un", -1, "Duex", -2, "Trois", -3), //
						4.6, Map.of("Dix", -10, "Onze", -11, "Douze", -12)));

		var instance = new TripleMapIterator<>(aList);
		int count = 0;
		while (instance.hasNext()) {
			var value = instance.next();
			count += 1;
		}
		assertEquals(12, count);
		assertThrows(NoSuchElementException.class, () -> {
			instance.next();
		});

	}

	@Test
	public void testEmpty() {
		var bigMap = new HashMap<Float, Map<Double, Map<String, Integer>>>();

		var instance = new TripleMapIterator<>(bigMap);
		int count = 0;
		while (instance.hasNext()) {
			var value = instance.next();
			count += 1;
		}
		assertEquals(0, count);
	}
}
