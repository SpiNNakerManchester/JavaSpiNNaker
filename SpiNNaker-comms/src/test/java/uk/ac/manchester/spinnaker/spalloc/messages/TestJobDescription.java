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
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;

import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author Christian
 */
public class TestJobDescription {
	@Test
	void testOneArg() throws IOException {
		var json = """
				{
					"job_id": 12345,
					"owner": "someone@manchester.ac.uk",
					"start_time": 1.537284307847865E9,
					"keepalive": 45.0,
					"state": 3,
					"power": true,
					"args": [1],
					"kwargs": {
						"tags": null,
						"max_dead_boards": 0,
						"machine": null,
						"min_ratio": 0.333,
						"max_dead_links": null,
						"require_torus": false
					},
					"allocated_machine_name": "Spin24b-223",
					"boards": [
						[1, 1, 2]
					],
					"keepalivehost": "130.88.198.171"
				}
				""";

		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, JobDescription.class);
		assertEquals(12345, fromJson.getJobID());
		assertEquals("someone@manchester.ac.uk", fromJson.getOwner());
		assertEquals(1.537284307847865E9, fromJson.getStartTime());
		assertEquals(45, fromJson.getKeepAlive());
		assertEquals(State.values()[3], fromJson.getState());
		assertEquals(true, fromJson.getPower());
		assertEquals(List.of(1), fromJson.getArgs());
		var map = fromJson.getKwargs();
		assertThat(map, IsMapContaining.hasEntry("tags", null));
		assertThat(map, IsMapContaining.hasEntry("max_dead_boards", 0));
		assertThat(map, IsMapContaining.hasEntry("machine", null));
		assertThat(map, IsMapContaining.hasEntry("min_ratio", 0.333));
		assertThat(map, IsMapContaining.hasEntry("max_dead_links", null));
		assertThat(map, IsMapContaining.hasEntry("require_torus", false));
		assertEquals("Spin24b-223", fromJson.getMachine());
		assertEquals(List.of(new BoardCoordinates(1, 1, 2)),
				fromJson.getBoards());
		assertEquals("130.88.198.171", fromJson.getKeepAliveHost());
	}

	@Test
	void testNulls() throws IOException {
		var json = """
				{
					"job_id": null
				}
				""";

		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, JobDescription.class);
		assertEquals(0, fromJson.getJobID());
		assertNotNull(fromJson.toString());
	}
}
