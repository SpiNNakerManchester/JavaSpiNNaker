/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
		var json = "{\"name\":\"power-monitor\","
				+ "\"tags\":[\"power-monitor\",\"machine-room\"],"
				+ "\"width\":1,\"height\":1,"
				+ "\"dead_boards\":[[0,0,1],[0,0,2]],\"dead_links\":[]}";
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
		var json = "{\"name\":\"power-monitor\","
				+ "\"tags\":[\"power-monitor\",\"machine-room\"],"
				+ "\"width\":1,\"height\":1,"
				+ "\"dead_boards\":[[1,2,3],[4,5,6]],"
				+ "\"dead_links\":[[7,8,9,10],[11,12,13,14]]}";
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
		assertEquals(7, fromJson.getDeadLinks().get(0).getX());
		assertEquals(14, fromJson.getDeadLinks().get(1).getLink());
		assertNotNull(fromJson.toString());
	}

	@Test
	void testNullJson() throws IOException {
		var json = "{\"name\":null}";
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
