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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author Christian
 */
public class TestBoardPhysicalCoordinates {

	@Test
	void testFromJson() throws IOException {
		var json = "[2, 4, 6]";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, BoardPhysicalCoordinates.class);
		assertEquals(2, fromJson.getCabinet());
		assertEquals(4, fromJson.getFrame());
		assertEquals(6, fromJson.getBoard());

		var direct = new BoardPhysicalCoordinates(2, 4, 6);
		assertEquals(direct, fromJson);
		assertEquals(direct.hashCode(), fromJson.hashCode());
		assertEquals(direct.toString(), fromJson.toString());
	}

}
