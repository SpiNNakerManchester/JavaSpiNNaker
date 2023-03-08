/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ListFetchingIterTest {
	@Test
	void basicUse() {
		var lfi = new ListFetchingIter<Integer>() {
			private int i = 0;

			@Override
			List<Integer> fetchNext() {
				i++;
				return List.of(i * 3, i * 3 + 1);
			}

			@Override
			boolean canFetchMore() {
				return i < 4;
			}
		};

		var all = new ArrayList<Integer>();
		lfi.forEachRemaining(all::addAll);
		assertEquals(List.of(3, 4, 6, 7, 9, 10, 12, 13), all);
	}

	@Test
	void throwing() {
		var lfi = new ListFetchingIter<Integer>() {
			private int i = 0;

			@Override
			List<Integer> fetchNext() throws IOException {
				if (i++ > 2) {
					throw new IOException("swallowed");
				}
				return List.of(i * 3, i * 3 + 1);
			}

			@Override
			boolean canFetchMore() {
				return i < 4;
			}
		};

		var all = new ArrayList<Integer>();
		lfi.forEachRemaining(all::addAll);
		assertEquals(List.of(3, 4, 6, 7, 9, 10), all);
	}
}
