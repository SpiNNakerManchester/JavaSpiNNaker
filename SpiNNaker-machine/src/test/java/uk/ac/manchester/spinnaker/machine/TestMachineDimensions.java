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
package uk.ac.manchester.spinnaker.machine;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian-B
 */
public class TestMachineDimensions {

	public TestMachineDimensions() {
	}

	@Test
	public void testEquals() {
		var m1 = new MachineDimensions(2, 3);
		var m2 = new MachineDimensions(2, 3);
		assertEquals(m1, m2);
		assertEquals(m1.hashCode(), m2.hashCode());
		assertEquals(m1.toString(), m2.toString());
	}

	@Test
	public void testNullEquals() {
		var m1 = new MachineDimensions(2, 3);
		MachineDimensions m2 = null;
		assertNotEquals(m1, m2);
		assertFalse(m1.equals(m2));
	}
}
