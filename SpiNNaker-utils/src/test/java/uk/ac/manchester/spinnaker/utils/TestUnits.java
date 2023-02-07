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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestUnits {

	public TestUnits() {
	}

	@Test
	public void testMilli() {
		assertEquals("25 ms", UnitConstants.formatDuration(25));
	}

	@Test
	public void testSecond() {
		assertEquals("2.003 s", UnitConstants.formatDuration(1000 * 2 + 3));
	}

	@Test
	public void testSecond2() {
		assertEquals("25.003 s", UnitConstants.formatDuration(1000 * 25 + 3));
	}

	@Test
	public void testMinutes() {
		assertEquals("2:00.003 m", UnitConstants.formatDuration(60000 * 2 + 3));
	}

	@Test
	public void testMinutes2() {
		assertEquals("23:00.003 m",
				UnitConstants.formatDuration(60000 * 23 + 3));
	}

	@Test
	public void testFewwHours() {
		long ms = 6 * 60 * 60 * 1000 + 34 * 60 * 1000 + 7 * 1000 + 45;
		assertEquals("6:34:07.045 h", UnitConstants.formatDuration(ms));
	}

	@Test
	public void testDuration() {
		long ms = 14 * 60 * 60 * 1000 + 34 * 60 * 1000 + 7 * 1000 + 45;
		assertEquals("14:34:07.045 h", UnitConstants.formatDuration(ms));
	}

	@Test
	public void testMoreThanaDay() {
		long ms = 87 * 60 * 60 * 1000 + 34 * 60 * 1000 + 7 * 1000 + 45;
		assertEquals("87:34:07.045 h", UnitConstants.formatDuration(ms));
	}
}
