/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.allocator;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

class ListFetchingIterTest {
	@Test
	void basicUse() {
		Iterator<List<Integer>> lfi = new ListFetchingIter<Integer>() {
			private int i = 0;

			@Override
			List<Integer> fetchNext() {
				i++;
				return asList(i * 3, i * 3 + 1);
			}

			@Override
			boolean canFetchMore() {
				return i < 4;
			}
		};

		List<Integer> all = new ArrayList<>();
		lfi.forEachRemaining(all::addAll);
		assertEquals(asList(3, 4, 6, 7, 9, 10, 12, 13), all);
	}

	@Test
	void throwing() {
		Iterator<List<Integer>> lfi = new ListFetchingIter<Integer>() {
			private int i = 0;

			@Override
			List<Integer> fetchNext() throws IOException {
				if (i++ > 2) {
					throw new IOException("swallowed");
				}
				return asList(i * 3, i * 3 + 1);
			}

			@Override
			boolean canFetchMore() {
				return i < 4;
			}
		};

		List<Integer> all = new ArrayList<>();
		lfi.forEachRemaining(all::addAll);
		assertEquals(asList(3, 4, 6, 7, 9, 10), all);
	}
}
