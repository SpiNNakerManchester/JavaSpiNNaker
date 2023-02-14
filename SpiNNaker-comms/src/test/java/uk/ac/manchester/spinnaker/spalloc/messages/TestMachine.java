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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author Christian
 */
public class TestMachine {

	@Test
	void testFromJson() throws IOException {
		var json = """
				{
					"name": "power-monitor",
					"tags": [
						"power-monitor",
						"machine-room"
					],
					"width": 1,
					"height":1,
					"dead_boards":[
						[0,0,1],
						[0,0,2]
					],
					"dead_links": []
				}"
				""";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, Machine.class);
		assertEquals("power-monitor", fromJson.getName());
		assertThat(fromJson.getTags(),
				contains("power-monitor", "machine-room"));
		assertEquals(1, fromJson.getWidth());
		assertEquals(1, fromJson.getHeight());
		assertThat(fromJson.getDeadBoards(), contains(
				new BoardCoordinates(0, 0, 1), new BoardCoordinates(0, 0, 2)));
		assertEquals(0, fromJson.getDeadLinks().size());
		assertNotNull(fromJson.toString());
	}

	@Test
	void testAssumedDeadLinks() throws IOException {
		var json = """
				{
					"name": "power-monitor",
					"tags": [
						"power-monitor",
						"machine-room"
					],
					"width": 1,
					"height": 1,
					"dead_boards": [
						[1,2,3],
						[4,5,6]
					],
					"dead_links": [
						[7,8,9,10],
						[11,12,13,14]
					]
				}
				""";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, Machine.class);
		assertEquals("power-monitor", fromJson.getName());
		assertThat(fromJson.getTags(),
				contains("power-monitor", "machine-room"));
		assertEquals(1, fromJson.getWidth());
		assertEquals(1, fromJson.getHeight());
		assertThat(fromJson.getDeadBoards(), contains(
				new BoardCoordinates(1, 2, 3), new BoardCoordinates(4, 5, 6)));
		assertEquals(2, fromJson.getDeadLinks().size());
		assertEquals(7, fromJson.getDeadLinks().get(0).x());
		assertEquals(14, fromJson.getDeadLinks().get(1).link());
		assertNotNull(fromJson.toString());
	}

	@Test
	void testNullJson() throws IOException {
		var json = """
				{
					"name": null
				}
				""";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, Machine.class);
		assertNull(fromJson.getName());
		assertTrue(fromJson.getTags().isEmpty(), "must have no tags");
		assertEquals(0, fromJson.getWidth());
		assertEquals(0, fromJson.getHeight());
		assertTrue(fromJson.getDeadBoards().isEmpty(),
				"must have no dead boards");
		assertTrue(fromJson.getDeadLinks().isEmpty(),
				"must have no dead links");
		assertNotNull(fromJson.toString());
	}
}
