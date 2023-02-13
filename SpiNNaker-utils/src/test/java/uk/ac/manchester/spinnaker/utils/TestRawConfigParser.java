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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestRawConfigParser {

	public TestRawConfigParser() {
	}

	@Test
	public void testSimple() {
		var url = TestRawConfigParser.class.getResource("/testconfig/test.cfg");
		var parser = new RawConfigParser(url);
		assertEquals((Integer) 5, parser.getInt("Machine", "version"));
		assertTrue(parser.getBoolean("Other", "alan_is_scotish"));
	}

}
