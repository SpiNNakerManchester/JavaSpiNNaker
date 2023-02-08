/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.iobuf;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;

import org.junit.jupiter.api.Test;

public class TestReplacer {
	private URL aplx = getClass().getResource("empty.aplx");

	@Test
	public void replace() {
		// Ignores bad line in dict file
		var replacer = new Replacer(aplx.getFile());

		assertEquals("abc-d-2-e-3-f-4",
				replacer.replace("1\u001e2\u001e3\u001e4"));
		assertEquals("abc,d,1,e,3,f,4",
				replacer.replace("2\u001e1\u001e3\u001e4"));
		assertEquals("1,2,3,4", replacer.replace("1,2,3,4"));
	}
}
