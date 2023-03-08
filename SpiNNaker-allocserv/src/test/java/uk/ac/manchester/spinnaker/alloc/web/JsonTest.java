/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.web;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.ON;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.json.JSONException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.model.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.model.ConnectionInfo;
import uk.ac.manchester.spinnaker.alloc.model.DownLink;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
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
			@Override
			public URI getRequestUri() {
				return uri;
			}

			@Override
			public UriBuilder getRequestUriBuilder() {
				return new UriBuilderImpl(uri);
			}

			@Override
			public URI getAbsolutePath() {
				return uri;
			}

			@Override
			public UriBuilder getAbsolutePathBuilder() {
				return new UriBuilderImpl(uri);
			}

			@Override
			public URI getBaseUri() {
				return uri;
			}

			@Override
			public UriBuilder getBaseUriBuilder() {
				return new UriBuilderImpl(uri);
			}
		};
	}

	private static class M extends StubMachine {
		@Override
		public int getId() {
			return 0;
		}

		@Override
		public String getName() {
			return "gorp";
		}

		@Override
		public Set<String> getTags() {
			return Set.of();
		}

		@Override
		public int getWidth() {
			return 1;
		}

		@Override
		public int getHeight() {
			return 1;
		}

		@Override
		public boolean isInService() {
			return false;
		}

		@Override
		public List<BoardCoords> getDeadBoards() {
			return List.of();
		}

		@Override
		public List<DownLink> getDownLinks() {
			return List.of();
		}

		@Override
		public String getRootBoardBMPAddress() {
			return "1.2.3.4";
		}

		@Override
		public List<Integer> getBoardNumbers() {
			return List.of();
		}

		@Override
		public List<Integer> getAvailableBoards() {
			return List.of();
		}

		@Override
		public List<Integer> getBoardNumbers(BMPCoords bmp) {
			return List.of();
		}

		@Override
		public boolean isHorizonallyWrapped() {
			return false;
		}

		@Override
		public boolean isVerticallyWrapped() {
			return false;
		}
	}

	private static class SM extends StubSubMachine {
		@Override
		public Machine getMachine() {
			return new M();
		}

		@Override
		public int getRootX() {
			return 0;
		}

		@Override
		public int getRootY() {
			return 0;
		}

		@Override
		public int getRootZ() {
			return 0;
		}

		@Override
		public int getWidth() {
			return 1;
		}

		@Override
		public int getHeight() {
			return 1;
		}

		@Override
		public int getDepth() {
			return 1;
		}

		@Override
		public List<ConnectionInfo> getConnections() {
			return List.of(new ConnectionInfo(ZERO_ZERO, "2.3.4.5"));
		}

		@Override
		public List<BoardCoordinates> getBoards() {
			return List.of(new BoardCoordinates(0, 0, 0));
		}
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
		void testSubMachineResponse() throws IOException, JSONException {
			var r = new SubMachineResponse(new SM(), null);
			JSONAssert.assertEquals(
					"{ \"depth\": 1, \"width\": 1, \"height\": 1,"
					+ "\"machine-name\": \"gorp\","
					+ "\"boards\": [[0, 0, 0]],"
					+ "\"connections\": [[[0, 0], \"2.3.4.5\"]] }",
					serialize(r), true);
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
