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
