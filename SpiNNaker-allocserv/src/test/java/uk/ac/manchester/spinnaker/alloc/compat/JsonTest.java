/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.compat;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
				.addModule(new JavaTimeModule())
				.addModule(new Jdk8Module())
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
			var d = new BoardCoordinates(0, 1, 2);
			JSONAssert.assertEquals("[0, 1, 2]", serialize(d), true);
		}

		@Test
		void testBoardPhysicalCoordinates() throws IOException, JSONException {
			var r = new BoardPhysicalCoordinates(0, 1, 2);
			JSONAssert.assertEquals("[0, 1, 2]", serialize(r), true);
		}

		@Test
		void testJobMachineInfo() throws IOException, JSONException {
			var r = new JobMachineInfo(0, 0,
					List.of(new Connection(ZERO_ZERO, "2.3.4.5")),
					"gorp", List.of(new BoardCoordinates(0, 1, 2)));

			JSONAssert.assertEquals("""
					{
						'boards': [
							[0,1,2]
						],
						'connections': [
							[[0, 0], '2.3.4.5']
						],
						'width': 0,
						'height': 0,
						'machine_name': 'gorp'
					}
					""", serialize(r), true);
		}

		@Test
		void testJobState() throws IOException, JSONException {
			var r = new JobState.Builder();
			r.setState(State.POWER);
			r.setStartTime(123);
			r.setPower(false);
			r.setReason("gorp");
			r.setKeepalive(321);
			r.setKeepalivehost("127.0.0.1");

			JSONAssert.assertEquals("""
					{
						'state': 2,
						'start_time': 123,
						'power': false,
						'reason': 'gorp',
						'keepalive': 321,
						'keepalivehost': '127.0.0.1'
					}
					""", serialize(r.build()), true);
		}

		@Test
		void testJobDescription() throws IOException, JSONException {
			var r = new JobDescription.Builder();
			r.setJobID(1);
			r.setArgs(List.of(0));
			r.setKwargs(Map.of());
			r.setKeepAlive(123);
			r.setKeepAliveHost("127.0.0.1");
			r.setMachine("foo");
			r.setOwner("bar");
			r.setPower(false);
			r.setStartTime(321.);
			r.setState(State.POWER);

			JSONAssert.assertEquals("""
					[
						{
							'allocated_machine_name': 'foo',
							'args': [0],
							'boards': [],
							'job_id': 1,
							'keepalive': 123,
							'keepalivehost': '127.0.0.1',
							'kwargs': {},
							'owner': 'bar',
							'power': false,
							'reason': null,
							'state': 2,
							'start_time': 321
						}
					]
					""", serialize(new JobDescription[] {r.build()}), true);
		}

		@Test
		void testMachine() throws IOException, JSONException {
			var r = new Machine("gorp", List.of("foo", "bar"), 0, 0, null,
					null);

			JSONAssert.assertEquals("""
					[
						{
							'name': 'gorp',
							'tags': ['foo', 'bar'],
							'dead_boards': [],
							'dead_links': [],
							'height': 0,
							'width': 0
						}
					]
					""", serialize(new Machine[] {r}), true);
		}

		@Test
		void testWhereIs() throws IOException, JSONException {
			var r = new WhereIs(new ChipLocation(0, 0), 0,
					new ChipLocation(0, 0), new BoardCoordinates(0, 1, 2),
					"gorp", new ChipLocation(0, 0),
					new BoardPhysicalCoordinates(0, 1, 2));

			JSONAssert.assertEquals("""
					{
						'chip': [0, 0],
						'board_chip': [0, 0],
						'job_chip': [0, 0],
						'job_id': 0,
						'logical': [0, 1, 2],
						'physical': [0, 1, 2],
						'machine': 'gorp'
					}
					""", serialize(r), true);
		}
	}
}
