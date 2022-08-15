/*
 * Copyright (c) 2022 The University of Manchester
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.MatchesPattern.matchesPattern;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.ServiceVersion;
import uk.ac.manchester.spinnaker.alloc.TestSupport;

@SpringBootTest
@SpringJUnitWebConfig(TestSupport.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + V1CompatTest.DB,
	"spalloc.historical-data.path=" + V1CompatTest.HIST_DB,
	"spalloc.compat.thread-pool-size=10",
	"spalloc.compat.service-user=" + TestSupport.USER_NAME,
	"spalloc.compat.service-group=" + TestSupport.GROUP_NAME
})
class V1CompatTest extends TestSupport {
	/** The name of the database file. */
	static final String DB = "target/compat_test.sqlite3";

	/** The name of the database file. */
	static final String HIST_DB = "target/compat_test-hist.sqlite3";

	private V1CompatService.TestAPI testAPI;

	@BeforeAll
	static void clearDB() throws IOException {
		killDB(DB);
	}

	@BeforeEach
	@SuppressWarnings("deprecation")
	void checkSetup(@Autowired V1CompatService compat) {
		assumeTrue(db != null, "spring-configured DB engine absent");
		setupDB1();
		testAPI = compat.getTestApi();
	}

	private void withInstance(
			BiConsumer<PrintWriter, NonThrowingLineReader> act)
			throws Exception {
		try (PipedWriter to = new PipedWriter();
				PipedReader from = new PipedReader()) {
			Future<?> f = testAPI.launchInstance(to, from);
			try {
				act.accept(new PrintWriter(to),
						new NonThrowingLineReader(from));
			} finally {
				if (!f.cancel(true)) {
					System.err.println("cancel failed?");
				}
			}
		}
	}

	// The representation of void
	private static final String VOID_RESPONSE = "{\"return\":null}";

	private static final String CHIP_LOCATION =
			"{\"return\":{\"job_chip\":null,"
					+ "\"job_id\":null,\"chip\":[0,0],\"logical\":[0,0,0],"
					+ "\"machine\":\"foo_machine\",\"board_chip\":[0,0],"
					+ "\"physical\":[1,1,0]}}";

	// The actual tests

	@Test
	public void testMachineryTest() throws Exception {
		for (int i = 0; i < 100; i++) {
			withInstance((to, from) -> {
				to.println();
				from.readLine();
			});
		}
		for (int i = 0; i < 100; i++) {
			withInstance((to, from) -> {
				to.println();
			});
		}
	}

	@Nested
	class WithoutJob {
		@Test
		void version(@Autowired ServiceVersion version) throws Exception {
			String response = "{\"return\":\"" + version.getVersion() + "\"}";
			withInstance((to, from) -> {
				to.println("{\"command\":\"version\"}");
				assertEquals(response, from.readLine());
			});
		}

		@Test
		void listMachines() throws Exception {
			String machinesResponse = "{\"return\":[{\"name\":\"" + MACHINE_NAME
					+ "\",\"tags\":[],\"width\":1,\"height\":1,"
					+ "\"dead_boards\":[],\"dead_links\":[]}]}";
			withInstance((to, from) -> {
				to.println("{\"command\": \"list_machines\"}");
				assertEquals(machinesResponse, from.readLine());
				to.println("{\"command\": \"list_machines\", \"args\": [0]}");
				assertEquals(machinesResponse, from.readLine());
				// An exception
				to.println("{\"command\": \"list_machines\", \"args\": false}");
				String line = from.readLine();
				assertNotEquals(machinesResponse, line);
				assertTrue(line.startsWith("{\"exception\":"),
						() -> "expected exception in " + line);
			});
		}

		@Test
		void listJobs() throws Exception {
			String jobsResponse = "{\"return\":[]}";
			withInstance((to, from) -> {
				to.println("{\"command\": \"list_jobs\"}");
				assertEquals(jobsResponse, from.readLine());
			});
		}

		@Test
		void notifyJob() throws Exception {
			withInstance((to, from) -> {
				to.println("{\"command\": \"notify_job\"}");
				assertEquals(VOID_RESPONSE, from.readLine());
				to.println("{\"command\": \"no_notify_job\"}");
				assertEquals(VOID_RESPONSE, from.readLine());
			});
		}

		@Test
		void notifyMachine() throws Exception {
			withInstance((to, from) -> {
				to.println("{\"command\": \"notify_machine\"}");
				assertEquals(VOID_RESPONSE, from.readLine());
				to.println("{\"command\": \"no_notify_machine\"}");
				assertEquals(VOID_RESPONSE, from.readLine());
				to.println("{\"command\":\"notify_machine\",\"args\":[\""
						+ MACHINE_NAME + "\"]}");
				assertEquals(VOID_RESPONSE, from.readLine());
				to.println("{\"command\":\"no_notify_machine\",\"args\":[\""
						+ MACHINE_NAME + "\"]}");
				assertEquals(VOID_RESPONSE, from.readLine());
			});
		}

		@Test
		void whereIs() throws Exception {
			withInstance((to, from) -> {
				to.println("{\"command\": \"where_is\", \"kwargs\":{"
						+ "\"machine\": \"" + MACHINE_NAME + "\","
						+ "\"x\": 0, \"y\": 0, \"z\": 0 }}");
				assertEquals(CHIP_LOCATION, from.readLine());
				to.println("{\"command\": \"where_is\", \"kwargs\":{"
						+ "\"machine\": \"" + MACHINE_NAME + "\","
						+ "\"cabinet\": 1, \"frame\": 1, \"board\": 0 }}");
				assertEquals(CHIP_LOCATION, from.readLine());
				to.println("{\"command\": \"where_is\", \"kwargs\":{"
						+ "\"machine\": \"" + MACHINE_NAME + "\","
						+ "\"chip_x\": 0, \"chip_y\": 0 }}");
				assertEquals(CHIP_LOCATION, from.readLine());
			});
		}

		@Test
		void getBoardAtPosition() throws Exception {
			// Physical->Logical map
			String response = "{\"return\":[0,0,0]}";
			withInstance((to, from) -> {
				to.println("{\"command\":\"get_board_at_position\",\"kwargs\":{"
						+ "\"machine_name\":\"" + MACHINE_NAME
						// Misnamed params if you ask me: cabinet, frame, board
						+ "\",\"x\":1,\"y\":1,\"z\":0}}");
				assertEquals(response, from.readLine());
			});
		}

		@Test
		void getBoardPosition() throws Exception {
			// Logical->Physical map
			String response = "{\"return\":[1,1,0]}";
			withInstance((to, from) -> {
				to.println("{\"command\":\"get_board_position\",\"kwargs\":{"
						+ "\"machine_name\":\"" + MACHINE_NAME
						+ "\",\"x\":0,\"y\":0,\"z\":0}}");
				assertEquals(response, from.readLine());
			});
		}
	}

	@Nested
	class WithJob {
		@Test
		void getJobState() throws Exception {
			withInstance((to, from) -> {
				withStandardAllocatedJob((p, jobId) -> {
					to.println("{\"command\":\"get_job_state\",\"args\":["
							+ jobId + "]}");
					assertThat("got job state", from.readLine(), matchesPattern(
							"\\{\"return\":\\{\"state\":1,\"power\":false,"
									+ ".*\\}\\}"));
				});
			});
		}

		@Test
		void getJobMachineInfo() throws Exception {
			withInstance((to, from) -> {
				withStandardAllocatedJob((p, jobId) -> {
					to.println("{\"command\":\"get_job_machine_info\","
							+ "\"args\":[" + jobId + "]}");
					assertThat("got job state", from.readLine(), matchesPattern(
							"\\{\"return\":\\{.*\"machine_name\":\""
									+ MACHINE_NAME + "\",.*\\}\\}"));
				});
			});
		}

		@Test
		void jobKeepalive() throws Exception {
			withInstance((to, from) -> {
				withStandardAllocatedJob((p, jobId) -> {
					to.println("{\"command\":\"job_keepalive\","
							+ "\"args\":[" + jobId + "]}");
					assertEquals(VOID_RESPONSE, from.readLine());
				});
			});
		}

		@Test
		void jobNotify() throws Exception {
			withInstance((to, from) -> {
				withStandardAllocatedJob((p, jobId) -> {
					to.println("{\"command\":\"notify_job\","
							+ "\"args\":[" + jobId + "]}");
					assertEquals(VOID_RESPONSE, from.readLine());
					to.println("{\"command\":\"no_notify_job\","
							+ "\"args\":[" + jobId + "]}");
					assertEquals(VOID_RESPONSE, from.readLine());
				});
			});
		}

		@Test
		void jobPower() throws Exception {
			withInstance((to, from) -> {
				withStandardAllocatedJob((p, jobId) -> {
					to.println("{\"command\":\"power_off_job_boards\","
							+ "\"args\":[" + jobId + "]}");
					assertEquals(VOID_RESPONSE, from.readLine());
				});
			});
		}

		@Test
		void whereIs() throws Exception {
			withInstance((to, from) -> {
				withStandardAllocatedJob((p, jobId) -> {
					to.println("{\"command\":\"where_is\","
							+ "\"kwargs\":{\"job_id\":" + jobId
							+ ",\"chip_x\":0,\"chip_y\":0}}");
					assertEquals("{\"return\":{\"job_chip\":[0,0],"
							+ "\"job_id\":" + jobId
							+ ",\"chip\":[0,0],\"logical\":[0,0,0],"
							+ "\"machine\":\"" + MACHINE_NAME + "\","
							+ "\"board_chip\":[0,0],"
							+ "\"physical\":[1,1,0]}}", from.readLine());
				});
			});
		}

		private String create(PrintWriter to, NonThrowingLineReader from,
				int... args) {
			to.println("{\"command\":\"create_job\",\"args\":"
					+ Arrays.toString(args) + ",\"kwargs\":{\"owner\":\"gorp\","
					+ "\"machine\":\"" + MACHINE_NAME + "\"}}");
			String line = from.readLine();
			Matcher m = compile("\\{\"return\":(\\d+)\\}").matcher(line);
			assertTrue(m.matches(),
					() -> format("'%s' doesn't match against '%s'", m.pattern(),
							line));
			return m.group(1);
		}

		private void destroy(PrintWriter to, NonThrowingLineReader from,
				Object jobId) {
			to.println("{\"command\":\"destroy_job\",\"args\":[" + jobId
					+ "],\"kwargs\":{\"reason\":\"whatever\"}}");
			assertEquals(VOID_RESPONSE, from.readLine());
		}

		@Test
		void jobCreateDelete() throws Exception {
			withInstance((to, from) -> {
				String jobId = create(to, from);
				destroy(to, from, jobId);

				jobId = create(to, from, 1);
				destroy(to, from, jobId);

				jobId = create(to, from, 1, 1);
				destroy(to, from, jobId);

				jobId = create(to, from, 0, 0, 0);
				destroy(to, from, jobId);
			});
		}

		@Test
		void compound() throws Exception {
			withInstance((to, from) -> {
				String jobId = create(to, from, 1, 1);

				to.println("{\"command\": \"list_jobs\"}");
				String jobs = from.readLine();
				assertThat("got job in list", jobs, matchesPattern(
						"\\{\"return\":\\[\\{.*\"job_id\":"
								+ jobId + ",.*\\}\\]\\}"));
				assertThat(jobs, containsString("\"args\":[1,1]"));

				destroy(to, from, jobId);

				to.println("{\"command\": \"list_jobs\"}");
				assertEquals("{\"return\":[]}", from.readLine());
			});
		}
	}
}

/** A buffered reader that doesn't throw if it gets an error reading a line. */
class NonThrowingLineReader extends BufferedReader {
	NonThrowingLineReader(PipedReader r) {
		super(r);
	}

	@Override
	public String readLine() {
		try {
			return super.readLine();
		} catch (IOException e) {
			return "THREW " + e;
		}
	}
}
