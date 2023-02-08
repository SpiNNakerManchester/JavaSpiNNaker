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
		var aMap = Map.of(23.2, Map.of("One", 1, "Two", 2, "Three", 3));

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
		var aMap = Map.of(//
				23.2, Map.of("One", 1, "Two", 2, "Three", 3), //
				43.6, Map.of("Ten", 10, "Eleven", 11, "Twelve", 12));

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
		// Compiler solver needs type hint
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
		// Compiler solver needs type hint
		var aMap = Map.of(343.2, new HashMap<String, Integer>(), //
				23.2, Map.of("One", 1, "Two", 2, "Three", 3), //
				43.6, Map.of("Ten", 10, "Eleven", 11, "Twelve", 12));

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
