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
package uk.ac.manchester.spinnaker.alloc.web;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.ON;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Optional;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.json.JSONException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

class JsonTest {
	private final ObjectMapper mapper;

	JsonTest() {
		// Set up the mapper in the same way that ServiceConfig does
		mapper = JsonMapper.builder().findAndAddModules()
				.disable(WRITE_DATES_AS_TIMESTAMPS)
				.propertyNamingStrategy(KEBAB_CASE).build();
	}

	private String serialize(Object obj) throws IOException {
		return mapper.writeValueAsString(obj);
	}

	private <T> T deserialize(String string, Class<T> cls) throws IOException {
		return mapper.readValue(string, cls);
	}

	private static UriInfo stubBuilder(String base) throws URISyntaxException {
		var uri = new URI(base);

		return new StubUriInfo() {
			UriBuilder ub = new UriBuilderImpl(uri);

			@Override
			public URI getRequestUri() {
				return uri;
			}

			@Override
			public UriBuilder getRequestUriBuilder() {
				return ub;
			}

			@Override
			public URI getAbsolutePath() {
				return uri;
			}

			@Override
			public UriBuilder getAbsolutePathBuilder() {
				return ub;
			}

			@Override
			public URI getBaseUri() {
				return uri;
			}

			@Override
			public UriBuilder getBaseUriBuilder() {
				return ub;
			}
		};
	}

	@Nested
	class Serialization {
		@Test
		void testServiceDescription() throws IOException, JSONException {
			var d = new ServiceDescription();
			d.setVersion(new Version("1.2.3"));
			JSONAssert.assertEquals(
					"{ \"version\": { \"major-version\": 1,"
							+ "\"minor-version\": 2, \"revision\": 3 } }",
					serialize(d), false);
		}

		@Test
		void testStateResponse() throws IOException, JSONException {
			var r = new JobStateResponse();
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
		void testWhereIsResponse()
				throws IOException, JSONException, URISyntaxException {
			var loc = new BoardLocation() {
				@Override
				public ChipLocation getBoardChip() {
					return new ChipLocation(3, 4);
				}

				@Override
				public ChipLocation getChipRelativeTo(ChipLocation rootChip) {
					return new ChipLocation(11, 12);
				}

				@Override
				public String getMachine() {
					return "gorp";
				}

				@Override
				public BoardCoordinates getLogical() {
					return new BoardCoordinates(5, 6, 0);
				}

				@Override
				public BoardPhysicalCoordinates getPhysical() {
					return new BoardPhysicalCoordinates(7, 8, 9);
				}

				@Override
				public ChipLocation getChip() {
					return new ChipLocation(1, 2);
				}

				@Override
				public Job getJob() {
					return new StubJob() {
						@Override
						public int getId() {
							return 12345;
						}

						@Override
						public Optional<ChipLocation> getRootChip() {
							return Optional.of(new ChipLocation(0, 0));
						}
					};
				}
			};
			var r = new WhereIsResponse(loc, stubBuilder("http://localhost/"));
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
		void testCreateJobRequestSimple() throws IOException {
			var obj = "{\"owner\":\"bob\", \"keepalive-interval\":\"PT30S\"}";
			var cjr = deserialize(obj, CreateJobRequest.class);
			assertNotNull(cjr);
			assertEquals("bob", cjr.owner);
			assertNotNull(cjr.keepaliveInterval);
			assertEquals(30, cjr.keepaliveInterval.getSeconds());
			assertNull(cjr.dimensions);
		}

		@Test
		void testCreateJobRequestComplex() throws IOException {
			var obj = "{\"owner\": \"bob\", \"keepalive-interval\": \"PT30S\", "
					+ "\"dimensions\": {\"width\": 1, \"height\": 2}, "
					+ "\"tags\": [\"a\", \"b\"], "
					+ "\"max-dead-boards\": 77, "
					+ "\"machine-name\": \"gorp\"}";
			var cjr = deserialize(obj, CreateJobRequest.class);
			assertNotNull(cjr);
			assertEquals("bob", cjr.owner);
			assertNotNull(cjr.keepaliveInterval);
			assertEquals(30, cjr.keepaliveInterval.getSeconds());
			assertNotNull(cjr.dimensions);
			assertEquals(1, cjr.dimensions.width);
			assertNotNull(cjr.tags);
			assertEquals(2, cjr.tags.size());
			assertEquals("a", cjr.tags.get(0));
			assertEquals("gorp", cjr.machineName);
			assertEquals(77, cjr.maxDeadBoards);
		}

		@Test
		void testPowerRequest() throws IOException {
			var obj = "{\"power\": \"ON\"}";
			var mp = deserialize(obj, MachinePower.class);
			assertEquals(ON, mp.getPower());
		}
	}
}
