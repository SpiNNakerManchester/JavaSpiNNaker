/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.compat;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.slf4j.LoggerFactory.getLogger;
import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.ThreadUtils.sleepQuietly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static uk.ac.manchester.spinnaker.alloc.compat.V1TaskImpl.VERSION;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

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
@Execution(SAME_THREAD)
class V1CompatTest extends TestSupport {
	// Take care in this class: JSON documents must be SINGLE LINE too

	/** The name of the database file. */
	static final String DB = "target/compat_test.sqlite3";

	/** The name of the database file. */
	static final String HIST_DB = "target/compat_test-hist.sqlite3";

	private V1CompatService.TestAPI testAPI;

	@BeforeEach
	@SuppressWarnings("deprecation")
	@Timeout(1)
	void checkSetup(@Autowired V1CompatService compat) throws IOException {
		assumeTrue(db != null, "spring-configured DB engine absent");
		killDB();
		setupDB1();
		testAPI = compat.getTestApi();
	}

	private void withInstance(String test,
			BiConsumer<PrintWriter, NonThrowingLineReader> act)
			throws Exception {
		Future<?> f = null;
		try (var to = new PipedWriter(); var from = new PipedReader()) {
			log.info("starting instance for {}", test);
			f = testAPI.launchInstance(to, from);
			log.debug("running test body {}", test);
			act.accept(new PrintWriter(to),
					new NonThrowingLineReader(from));
			// Stop the instance
			log.debug("stopping instance for {}", test);
		} finally {
			sleepQuietly(Duration.of(20, MILLIS));
			if (f != null && !f.isDone()) {
				sleepQuietly(Duration.of(80, MILLIS));
				if (f.cancel(true)) {
					log.warn("cancelled instance task");
				} else {
					log.debug("task cancel failed; probably already finished");
				}
			}
		}
		/*
		 * Clear the interrupted status. If we got here (i.e., we're NOT in a
		 * finally block) we weren't interrupted in a way that matters, so we
		 * mustn't have the thread marked as interrupted here or JUnit throws a
		 * very strange failure.
		 */
		Thread.interrupted();
		log.info("stopped instance for {}", test);
	}

	// The representation of void
	private static final String VOID_RESPONSE = "{\"return\":null}";

	private static final String CHIP_LOCATION =
			"{\"return\":{\"job_chip\":null,"
					+ "\"job_id\":null,\"chip\":[0,0],\"logical\":[0,0,0],"
					+ "\"machine\":\"foo_machine\",\"board_chip\":[0,0],"
					+ "\"physical\":[1,1,0]}}";

	private static String create(PrintWriter to, NonThrowingLineReader from,
			int... args) {
		to.println("{\"command\":\"create_job\",\"args\":"
				+ Arrays.toString(args) + ",\"kwargs\":{\"owner\":\"gorp\","
				+ "\"machine\":\"" + MACHINE_NAME + "\"}}");
		var line = from.readLine();
		var m = compile("\\{\"return\":(\\d+)\\}").matcher(line);
		assertTrue(m.matches(),
				() -> format("'%s' doesn't match against '%s'", m.pattern(),
						line));
		return m.group(1);
	}

	private static void destroy(PrintWriter to, NonThrowingLineReader from,
			Object jobId) {
		to.println("{\"command\":\"destroy_job\",\"args\":[" + jobId
				+ "],\"kwargs\":{\"reason\":\"whatever\"}}");
		assertEquals(VOID_RESPONSE, from.readLine());
	}

	// The actual tests

	private static final int MACHINERY_TEST_SIZE = 50;

	@Test
	@Timeout(15)
	public void testMachineryTestBidirectional() throws Exception {
		log.info("starting MachineryTestBidirectional");
		for (int i = 0; i < MACHINERY_TEST_SIZE; i++) {
			withInstance("MachineryTestBidirectional " + i, (to, from) -> {
				to.println();
				from.readLine();
			});
		}
		log.info("finished MachineryTestBidirectional");
	}

	@Test
	@Timeout(15)
	public void testMachineryTestUnidirectional() throws Exception {
		for (int i = 0; i < MACHINERY_TEST_SIZE; i++) {
			withInstance("MachineryTestUnidirectional " + i, (to, from) -> {
				to.println();
			});
		}
	}

	/** Tests that don't require an existing job. */
	@Nested
	class WithoutJob {
		@Test
		@Timeout(5)
		void version() throws Exception {
			var response = "{\"return\":\"" + VERSION + "\"}";
			withInstance("nojob.version", (to, from) -> {
				to.println("{\"command\":\"version\"}");
				assertEquals(response, from.readLine());
			});
		}

		@Test
		@Timeout(5)
		void listMachines() throws Exception {
			var machinesResponse = "{\"return\":[{\"name\":\"" + MACHINE_NAME
					+ "\",\"tags\":[],\"width\":1,\"height\":1,"
					+ "\"dead_boards\":[],\"dead_links\":[]}]}";
			withInstance("nojob.listMachines", (to, from) -> {
				to.println("{\"command\": \"list_machines\"}");
				assertEquals(machinesResponse, from.readLine());
				to.println("{\"command\": \"list_machines\", \"args\": [0]}");
				assertEquals(machinesResponse, from.readLine());
				// An exception
				to.println("{\"command\": \"list_machines\", \"args\": false}");
				var line = from.readLine();
				assertNotEquals(machinesResponse, line);
				assertTrue(line.startsWith("{\"exception\":"),
						() -> "expected exception in " + line);
			});
		}

		@Test
		@Timeout(5)
		void listJobs() throws Exception {
			var jobsResponse = "{\"return\":[]}";
			withInstance("nojob.listJobs", (to, from) -> {
				to.println("{\"command\": \"list_jobs\"}");
				assertEquals(jobsResponse, from.readLine());
			});
		}

		@Test
		@Timeout(5)
		void notifyJob() throws Exception {
			withInstance("nojob.notifyJob", (to, from) -> {
				to.println("{\"command\": \"notify_job\"}");
				assertEquals(VOID_RESPONSE, from.readLine());
				to.println("{\"command\": \"no_notify_job\"}");
				assertEquals(VOID_RESPONSE, from.readLine());
			});
		}

		@Test
		@Timeout(5)
		void notifyMachine() throws Exception {
			withInstance("nojob.notifyMachine", (to, from) -> {
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
		@Timeout(5)
		void whereIs() throws Exception {
			withInstance("nojob.whereis", (to, from) -> {
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
		@Timeout(5)
		void getBoardAtPosition() throws Exception {
			// Physical->Logical map
			var response = "{\"return\":[0,0,0]}";
			withInstance("nojob.getAtPos", (to, from) -> {
				to.println("{\"command\":\"get_board_at_position\",\"kwargs\":{"
						+ "\"machine_name\":\"" + MACHINE_NAME
						// Misnamed params if you ask me: cabinet, frame, board
						+ "\",\"x\":1,\"y\":1,\"z\":0}}");
				assertEquals(response, from.readLine());
			});
		}

		@Test
		@Timeout(5)
		void getBoardPosition() throws Exception {
			// Logical->Physical map
			var response = "{\"return\":[1,1,0]}";
			withInstance("nojob.boardPos", (to, from) -> {
				to.println("{\"command\":\"get_board_position\",\"kwargs\":{"
						+ "\"machine_name\":\"" + MACHINE_NAME
						+ "\",\"x\":0,\"y\":0,\"z\":0}}");
				assertEquals(response, from.readLine());
			});
		}

		@Test
		@Timeout(5)
		void jobCreateDelete() throws Exception {
			Logger log = getLogger(V1CompatTest.class);
			withInstance("nojob.createDelete", (to, from) -> {
				var jobId = create(to, from);
				log.info("created(1) with ID={}", jobId);
				destroy(to, from, jobId);
				log.info("destroyed(1) ID={}", jobId);

				jobId = create(to, from, 1);
				log.info("created(2) with ID={}", jobId);
				destroy(to, from, jobId);
				log.info("destroyed(2) ID={}", jobId);

				jobId = create(to, from, 1, 1);
				log.info("created(3) with ID={}", jobId);
				destroy(to, from, jobId);
				log.info("destroyed(3) ID={}", jobId);

				jobId = create(to, from, 0, 0, 0);
				log.info("created(4) with ID={}", jobId);
				destroy(to, from, jobId);
				log.info("destroyed(4) ID={}", jobId);

				to.println("{\"command\":\"create_job\",\"args\":[0,0,0,0],"
						+ "\"kwargs\":{\"owner\":\"gorp\"," + "\"machine\":\""
						+ MACHINE_NAME + "\"}}");
				assertEquals(
						"{\"exception\":"
								+ "\"unsupported number of arguments: 4\"}",
						from.readLine());
			});
		}

		@Test
		@Timeout(5)
		void compound() throws Exception {
			withInstance("nojob.compound", (to, from) -> {
				var jobId = create(to, from, 1, 1);

				try {
					to.println("{\"command\": \"list_jobs\"}");
					var jobs = from.readLine();
					assertThat("got job in list", jobs,
							matchesPattern("\\{\"return\":\\[\\{.*\"job_id\":"
									+ jobId + ",.*\\}\\]\\}"));
					assertThat(jobs, containsString("\"args\":[1,1]"));
				} finally {
					destroy(to, from, jobId);
				}

				to.println("{\"command\": \"list_jobs\"}");
				assertEquals("{\"return\":[]}", from.readLine());
			});
		}
	}

	/** Tests against an existing job. */
	@Nested
	@TestInstance(PER_CLASS)
	class WithJob {
		private int jobId;

		private String expectedNotification;

		// Make an allocated job for us to work with
		@BeforeEach
		@Timeout(5)
		void setupJob() {
			jobId = makeJob();
			expectedNotification = "{\"jobs_changed\":[" + jobId + "]}";
			db.executeVoid(c -> {
				allocateBoardToJob(c, BOARD, jobId);
				setAllocRoot(c, jobId, BOARD);
			});
		}

		// Get rid of the allocated job
		@AfterEach
		@Timeout(5)
		void teardownJob() {
			db.executeVoid(c -> {
				allocateBoardToJob(c, BOARD, null);
				setAllocRoot(c, jobId, null);
			});
			nukeJob(jobId);
		}

		private String readReply(NonThrowingLineReader from) {
			// Skip over any notifications
			String r;
			do {
				r = from.readLine();
			} while (r.equals(expectedNotification));
			return r;
		}

		@Test
		@Timeout(5)
		void getJobState() throws Exception {
			withInstance("job.state", (to, from) -> {
				to.println("{\"command\":\"get_job_state\",\"args\":[" + jobId
						+ "]}");
				assertThat("got job state", readReply(from),
						// state could be QUEUED or POWER; either is fine
						matchesPattern("\\{\"return\":\\{\"state\":[12],"
								+ "\"power\":false,.*\\}\\}"));
			});
		}

		@Test
		@Timeout(5)
		void getJobMachineInfo() throws Exception {
			withInstance("job.machineInfo", (to, from) -> {
				to.println("{\"command\":\"get_job_machine_info\",\"args\":["
						+ jobId + "]}");
				assertThat("got job state", readReply(from),
						matchesPattern("\\{\"return\":\\{.*\"machine_name\":\""
								+ MACHINE_NAME + "\",.*\\}\\}"));
			});
		}

		@Test
		@Timeout(5)
		void jobKeepalive() throws Exception {
			withInstance("job.keepalive", (to, from) -> {
				to.println("{\"command\":\"job_keepalive\",\"args\":[" + jobId
						+ "]}");
				assertEquals(VOID_RESPONSE, readReply(from));
			});
		}

		@Test
		@Timeout(5)
		void jobNotify() throws Exception {
			withInstance("job.notify", (to, from) -> {
				to.println("{\"command\":\"notify_job\",\"args\":[" + jobId
						+ "]}");
				assertEquals(VOID_RESPONSE, readReply(from));
				to.println("{\"command\":\"no_notify_job\",\"args\":[" + jobId
						+ "]}");
				assertEquals(VOID_RESPONSE, readReply(from));
			});
		}

		@Test
		@Timeout(5)
		void jobPower() throws Exception {
			withInstance("job.power", (to, from) -> {
				to.println("{\"command\":\"power_off_job_boards\","
						+ "\"args\":[" + jobId + "]}");
				assertEquals(VOID_RESPONSE, readReply(from));
			});
		}

		@Test
		@Timeout(5)
		void whereIs() throws Exception {
			withInstance("job.whereis", (to, from) -> {
				to.println("{\"command\":\"where_is\",\"kwargs\":{\"job_id\":"
						+ jobId + ",\"chip_x\":0,\"chip_y\":0}}");
				assertEquals("{\"return\":{\"job_chip\":[0,0],\"job_id\":"
						+ jobId + ",\"chip\":[0,0],\"logical\":[0,0,0],"
						+ "\"machine\":\"" + MACHINE_NAME + "\","
						+ "\"board_chip\":[0,0],\"physical\":[1,1,0]}}",
						readReply(from));
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
