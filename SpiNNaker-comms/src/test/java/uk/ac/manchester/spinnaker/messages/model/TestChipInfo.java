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
package uk.ac.manchester.spinnaker.messages.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

class TestChipInfo {
	private static final int SIZE = 256;

	@Test
	void testCreateWithBlankBuffer() {
		var b = ByteBuffer.allocate(SIZE);
		var c = new ChipInfo(b);
		assertEquals(0, c.getSDRAMClock());
	}

}
