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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestCounter {

	public TestCounter() {
	}

	/**
	 * Test of increment and get methods, of class Counter.
	 */
	@Test
	public void testIncrement() {
		var counter = new Counter();
		assertEquals(0, counter.get());
		counter.increment();
		counter.increment();
		assertEquals(2, counter.get());
	}

	@Test
	public void testAdd() {
		var counter = new Counter();
		assertEquals(0, counter.get());
		counter.add(4);
		counter.add(-2);
		assertEquals(2, counter.get());
	}
}
