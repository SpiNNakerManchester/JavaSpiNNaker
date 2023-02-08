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
package uk.ac.manchester.spinnaker.messages.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author micro
 */
public class TestVersion {

	@Test
	public void testThreeUnquoted() {
		var version = Version.parse("1.2.3");
		assertEquals(1, version.majorVersion());
		assertEquals(2, version.minorVersion());
		assertEquals(3, version.revision());
	}

	@Test
	public void testThreeQuoted() {
		var version = Version.parse("\"1.2.3\"");
		assertEquals(1, version.majorVersion());
		assertEquals(2, version.minorVersion());
		assertEquals(3, version.revision());
	}

	@Test
	public void testTwoUnquoted() {
		var version = Version.parse("1.2");
		assertEquals(1, version.majorVersion());
		assertEquals(2, version.minorVersion());
		assertEquals(0, version.revision());
	}

	@Test
	public void testOneQuoted() {
		var version = Version.parse("\"1\"");
		assertEquals(1, version.majorVersion());
		assertEquals(0, version.minorVersion());
		assertEquals(0, version.revision());
	}
}
