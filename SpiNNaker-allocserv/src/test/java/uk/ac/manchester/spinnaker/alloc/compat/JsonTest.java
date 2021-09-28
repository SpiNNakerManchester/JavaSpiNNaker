/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.compat;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.json.JSONException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.Connection;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.Machine;
import uk.ac.manchester.spinnaker.spalloc.messages.State;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;

class JsonTest {
	private final ObjectMapper mapper;

	JsonTest() {
		// Set up the mapper in the same way that ServiceConfig does
		mapper = JsonMapper.builder().findAndAddModules()
				.disable(WRITE_DATES_AS_TIMESTAMPS)
				.propertyNamingStrategy(SNAKE_CASE).build();
	}

	private String serialize(Object obj) throws IOException {
		return mapper.writeValueAsString(obj);
	}

	@Nested
	class Serialization {
		@Test
		void testBoardCoordinates() throws IOException, JSONException {
			BoardCoordinates d = new BoardCoordinates(0, 1, 2);
			JSONAssert.assertEquals("[0, 1, 2]", serialize(d), true);
		}

		@Test
		void testBoardPhysicalCoordinates() throws IOException, JSONException {
			BoardPhysicalCoordinates r = new BoardPhysicalCoordinates(0, 1, 2);
			JSONAssert.assertEquals(
					"[0, 1, 2]",
					serialize(r), true);
		}

		@Test
		void testJobMachineInfo() throws IOException, JSONException {
			JobMachineInfo r = new JobMachineInfo();
			r.setMachineName("gorp");
			r.setBoards(Arrays.asList(new BoardCoordinates(0, 1, 2)));
			r.setConnections(Arrays
					.asList(new Connection(new ChipLocation(0, 0), "foo.bar")));
			JSONAssert.assertEquals(
					"{ 'boards': [[0,1,2]], "
							+ "'connections': [[[0,0],'foo.bar']], "
							+ "'width': 0, "
							+ "'height': 0, "
							+ "'machine_name': 'gorp' }",
					serialize(r), true);
		}

		@Test
		void testJobState() throws IOException, JSONException {
			JobState r = new JobState();
			r.setPower(false);
			r.setReason("gorp");
			r.setState(State.POWER);
			r.setKeepalivehost("127.0.0.1");
			r.setStartTime(123);
			r.setKeepalive(321);
			JSONAssert.assertEquals(
					"{ 'state': 2, "
							+ "'start_time': 123, "
							+ "'power': false, "
							+ "'reason': 'gorp', "
							+ "'keepalive': 321, "
							+ "'keepalivehost': '127.0.0.1' }",
					serialize(r), true);
		}

		@Test
		void testJobDescription() throws IOException, JSONException {
			JobDescription[] r = new JobDescription[1];
			r[0] = new JobDescription();
			r[0].setJobID(1);
			r[0].setArgs(Arrays.asList(0));
			r[0].setKwargs(new HashMap<>());
			r[0].setKeepAlive(123);
			r[0].setKeepAliveHost("127.0.0.1");
			r[0].setMachine("foo");
			r[0].setOwner("bar");
			r[0].setPower(false);
			r[0].setStartTime(321);
			r[0].setState(State.POWER);
			JSONAssert.assertEquals(
					"[{ 'allocated_machine_name': 'foo', "
							+ "'args': [0], "
							+ "'boards': [], "
							+ "'job_id': 1, "
							+ "'keepalive': 123, "
							+ "'keepalivehost': '127.0.0.1', "
							+ "'kwargs': {}, "
							+ "'owner': 'bar', "
							+ "'power': false, "
							+ "'reason': null, "
							+ "'state': 2, "
							+ "'start_time': 321 "
							+ "}]",
					serialize(r), true);
		}

		@Test
		void testMachine() throws IOException, JSONException {
			Machine[] r = new Machine[1];
			r[0] = new Machine();
			r[0].setName("gorp");
			r[0].setTags(Arrays.asList("foo", "bar"));
			JSONAssert
					.assertEquals(
							"[{ 'name': 'gorp', 'tags': ['foo', 'bar'], "
									+ "'dead_boards': [], 'dead_links': [], "
									+ "'height': 0, 'width': 0 }]",
							serialize(r), true);
		}

		@Test
		void testWhereIs() throws IOException, JSONException {
			WhereIs r = new WhereIs(new ChipLocation(0, 0), 0,
					new ChipLocation(0, 0), new BoardCoordinates(0, 1, 2),
					"gorp", new ChipLocation(0, 0),
					new BoardPhysicalCoordinates(0, 1, 2));
			JSONAssert.assertEquals(
					"{'chip': [0,0], 'board_chip': [0,0],"
					+ "'job_chip': [0,0], 'job_id': 0,"
					+ "'logical': [0,1,2], 'physical': [0,1,2],"
					+ "'machine': 'gorp'}",
					serialize(r), true);
		}
	}
}
