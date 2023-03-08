/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.utils.DefaultMap.KeyAwareFactory;

/**
 *
 * @author Christian-B
 */
public class TestDefaultMap {

	public TestDefaultMap() {
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testUntyped() {
		var instance = new DefaultMap(ArrayList::new);
		var foo = instance.get("foo");
		assertTrue(foo instanceof ArrayList);
		var fooList = (ArrayList) foo;
		fooList.add("a");
		fooList.add(1);
	}

	@Test
	public void testTyped() {
		// Explicit to work around weird issue in Java 14 compiler
		Map<String, List<Integer>> instance = new DefaultMap<>(ArrayList::new);
		var foo = instance.get("foo");
		assertTrue(foo instanceof ArrayList);
		// foo.add("a");
		foo.add(1);
		var bar = instance.get("bar");
		assertNotEquals(foo, bar);
		assertEquals(1, foo.size());
		assertEquals(0, bar.size());
		var bar2 = instance.get("bar");
		bar2.add(123);
		assertEquals(bar, bar2);
	}

	/**
	 * Instead it demonstrates the if you pass in an Object the same instance of
	 * this Object is used every time.
	 */
	@Test
	public void testBad() {
		Map<String, List<Integer>> instance =
				DefaultMap.newMapWithDefault(new ArrayList<>());
		var foo = instance.get("one");
		foo.add(11);
		var bar = instance.get("two");
		bar.add(12);
		assertEquals(2, bar.size());
		assertTrue(foo == bar);
	}

	@Test
	public void testKeyAware() {
		DefaultMap<Integer, Integer> instance =
				DefaultMap.newAdvancedDefaultMap(new Doubler());
		var two = instance.get(1);
		assertEquals(2, two.intValue());
	}

	@Test
	public void testKeyAware2() {
		DefaultMap<Integer, Integer> instance =
				DefaultMap.newAdvancedDefaultMap(i -> i * 2);
		var two = instance.get(1);
		assertEquals(2, two.intValue());
	}

	public static class Doubler implements KeyAwareFactory<Integer, Integer> {
		@Override
		public Integer createValue(Integer key) {
			return Integer.valueOf(key.intValue() * 2);
		}
	}

}
