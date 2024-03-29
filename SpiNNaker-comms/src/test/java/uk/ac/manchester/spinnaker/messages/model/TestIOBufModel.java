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
package uk.ac.manchester.spinnaker.messages.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.ChipLocation;

class TestIOBufModel {

	@Test
	void test() throws UnsupportedEncodingException {
		var buf = "Everything failed on chip.".getBytes("ASCII");
		var b = new IOBuffer(new ChipLocation(1, 2).getScampCore(), buf);
		assertEquals(1, b.getX());
		assertEquals(2, b.getY());
		assertEquals(0, b.getP());
		assertEquals("Everything failed on chip.", b.getContentsString());
	}

}
