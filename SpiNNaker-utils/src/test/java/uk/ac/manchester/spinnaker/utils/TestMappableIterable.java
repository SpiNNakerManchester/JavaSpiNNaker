/*
 * Copyright (c) 2021 The University of Manchester
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

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
public class TestMappableIterable {
	private final List<Integer> values = List.of(1, 2, 3, 4, 5);

	private final List<Integer> values2 = List.of(1, 2, 3, 2, 1);

	private final List<Integer> values3 = List.of(5, 6, 7, 8, 9);

	private final List<Integer> empty = List.of();

	private static void assertUnreachable() {
		assertTrue(false, "unreachable code reached");
	}

	@Test
	public void testMap() {
		MappableIterable<Integer> mi = values::iterator;

		int i = 3;
		for (Integer value : mi.map(x -> x * 3)) {
			assertEquals(i, value);
			i += 3;
		}
		assertEquals(18, i);

		assertEquals(List.of('A', 'B', 'C', 'D', 'E'),
				mi.map(x -> (char) (x + 64)).toList());

		mi = () -> empty.iterator();
		i = 0;
		for (Integer value : mi.map(x -> 1 << x)) {
			i += value;
		}
		assertEquals(i, 0);
	}

	@Test
	public void testToListAndSet() {
		MappableIterable<Integer> mi = values2::iterator;

		assertEquals(values2, mi.toList());
		assertEquals(List.of(1, 2, 3), List.copyOf(mi.toSet()));
	}

	@Test
	public void testFilter() {
		MappableIterable<Integer> mi = values::iterator;

		int i = 1;
		for (Integer value : mi.filter(x -> (x & 1) > 0)) {
			assertEquals(i, value);
			i += 2;
		}
		assertEquals(i, 7);

		i = 2;
		for (Integer value : mi.filter(x -> (x & 1) == 0)) {
			assertEquals(i, value);
			i += 2;
		}
		assertEquals(i, 6);

		for (Integer value : mi.filter(x -> x < 1)) {
			assertUnreachable();
		}

		mi = () -> empty.iterator();
		for (Integer value : mi.filter(x -> {
			assertUnreachable();
			return true;
		})) {
			assertUnreachable();
		}
	}

	@Test
	public void testFirst() {
		MappableIterable<Integer> mi = values::iterator;

		var first = mi.first();
		assertTrue(first.isPresent());
		assertEquals(1, first.orElseThrow());

		var first3 = mi.first(3).toList();
		assertEquals(List.of(1, 2, 3), first3);

		mi = () -> empty.iterator();
		assertFalse(mi.first().isPresent());
	}

	@Test
	public void testNth() {
		MappableIterable<Integer> mi = values3::iterator;

		assertEquals(7, mi.nth(2).orElseThrow());
		assertFalse(mi.nth(7).isPresent());
	}
}
