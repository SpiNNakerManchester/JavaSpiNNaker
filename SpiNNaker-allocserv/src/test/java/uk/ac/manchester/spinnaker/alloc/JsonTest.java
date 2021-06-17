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
package uk.ac.manchester.spinnaker.alloc;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Instant;

import org.json.JSONException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.allocator.JobState;
import uk.ac.manchester.spinnaker.alloc.web.CreateJobRequest;
import uk.ac.manchester.spinnaker.alloc.web.ServiceDescription;
import uk.ac.manchester.spinnaker.alloc.web.JobStateResponse;
import uk.ac.manchester.spinnaker.alloc.web.WhereIsResponse;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

class JsonTest {
	private final ObjectMapper mapper;

	JsonTest() {
		// Set up the mapper in the same way that ServiceConfig does
		mapper = JsonMapper.builder()
			    .findAndAddModules()
			    .disable(WRITE_DATES_AS_TIMESTAMPS)
			    .propertyNamingStrategy(KEBAB_CASE)
			    .build();
	}

	private String serialize(Object obj) throws IOException {
		return mapper.writeValueAsString(obj);
	}

	private <T> T deserialize(String string, Class<T> cls) throws IOException {
		return mapper.readValue(string, cls);
	}

	@Nested
	class Serialization {
		@Test
		void testServiceDescription() throws IOException, JSONException {
			ServiceDescription d = new ServiceDescription();
			d.setVersion(new Version("1.2.3"));
			JSONAssert.assertEquals(
					"{ \"version\": { \"major-version\": 1,"
							+ "\"minor-version\": 2, \"revision\": 3 } }",
					serialize(d), false);
		}

		@Test
		void testStateResponse() throws IOException, JSONException {
			JobStateResponse r = new JobStateResponse();
			r.setState(JobState.READY);
			r.setStartTime(Instant.ofEpochSecond(1633954394));
			r.setKeepaliveHost("127.0.0.1");
			r.setOwner("gorp");
			JSONAssert.assertEquals(
					"{ \"state\": \"READY\", "
							+ "\"start-time\": \"2021-10-11T12:13:14Z\", "
							+ "\"owner\": \"gorp\", "
							+ "\"keepalive-host\": \"127.0.0.1\" }",
					serialize(r), false);
		}

		@Test
		void testWhereIsResponse() throws IOException, JSONException {
			WhereIsResponse r = new WhereIsResponse();
			r.jobId = 12345;
			r.machine = "gorp";
			r.chip = new ChipLocation(1, 2);
			r.boardChip = new ChipLocation(3, 4);
			r.jobChip = new ChipLocation(11, 12);
			r.logicalBoardCoordinates = new BoardCoordinates();
			r.logicalBoardCoordinates.setX(5);
			r.logicalBoardCoordinates.setY(6);
			r.logicalBoardCoordinates.setZ(0);
			r.physicalBoardCoordinates = new BoardPhysicalCoordinates();
			r.physicalBoardCoordinates.setCabinet(7);
			r.physicalBoardCoordinates.setFrame(8);
			r.physicalBoardCoordinates.setBoard(9);
			JSONAssert.assertEquals(
					"{ \"machine\": \"gorp\", \"chip\": [1, 2], "
							+ "\"job-id\": 12345, \"board-chip\": [3, 4],"
							+ "\"job-chip\": [11, 12],"
							+ "\"logical-board-coordinates\": [5, 6, 0],"
							+ "\"physical-board-coordinates\": [7, 8, 9] }",
					serialize(r), false);
		}
	}

	@Nested
	class Deserialization {
		@Test
		void testCreateJobRequest() throws IOException {
			String obj =
					"{\"owner\":\"bob\", \"keepalive-interval\":\"PT30S\"}";
			CreateJobRequest cjr = deserialize(obj, CreateJobRequest.class);
			assertNotNull(cjr);
			assertEquals("bob", cjr.owner);
			assertNotNull(cjr.keepaliveInterval);
			assertEquals(30, cjr.keepaliveInterval.getSeconds());
		}
	}
}
