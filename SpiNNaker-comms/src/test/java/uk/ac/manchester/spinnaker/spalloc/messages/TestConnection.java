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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author Christain
 */
public class TestConnection {

	@Test
	void testFromJson() throws IOException {
		var json = "[[2,4],\"6.8.10.12\"]";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, Connection.class);
		assertEquals(new ChipLocation(2, 4), fromJson.chip());
		assertEquals("6.8.10.12", fromJson.hostname());

		var direct = new Connection(new ChipLocation(2, 4), "6.8.10.12");
		assertEquals(direct, fromJson);
		assertEquals(direct.hashCode(), fromJson.hashCode());
		assertEquals(direct.toString(), fromJson.toString());
	}

	@Test
	void testNulls() throws IOException {
		var json = "[null,null]";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, Connection.class);
		assertNull(fromJson.chip());
		assertNull(fromJson.hostname());

		var direct = new Connection(null, null);
		assertEquals(direct, fromJson);
		assertEquals(direct.hashCode(), fromJson.hashCode());
		assertEquals(direct.toString(), fromJson.toString());
	}

}
