/*
 * Copyright (c) 2021 The University of Manchester
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
		assertEquals(List.of(1, 2, 3), new ArrayList<>(mi.toSet()));
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
