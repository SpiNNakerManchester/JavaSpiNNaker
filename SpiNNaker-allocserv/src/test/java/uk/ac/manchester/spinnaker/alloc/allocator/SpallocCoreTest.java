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
package uk.ac.manchester.spinnaker.alloc.allocator;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.BOARD;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.GROUP_NAME;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.MACHINE_NAME;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.USER_NAME;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.allocateBoardToJob;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.makeJob;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.setAllocRoot;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.setupDB1;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.OFF;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityUtils.inContext;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateBoard;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateDimensions;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateDimensionsAt;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateNumBoards;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.SubMachine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.model.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.model.JobDescription;
import uk.ac.manchester.spinnaker.alloc.model.MachineDescription;
import uk.ac.manchester.spinnaker.alloc.model.MachineDescription.JobInfo;
import uk.ac.manchester.spinnaker.alloc.model.MachineListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.security.Permit;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;

@SpringBootTest
@SpringJUnitWebConfig(SpallocCoreTest.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + SpallocCoreTest.DB,
	"spalloc.historical-data.path=" + SpallocCoreTest.HIST_DB
})
class SpallocCoreTest extends SQLQueries {
	private static final Logger log = getLogger(SpallocCoreTest.class);

	/** The name of the database file. */
	static final String DB = "target/spalloc_test.sqlite3";

	/** The name of the database file. */
	static final String HIST_DB = "target/spalloc_test-hist.sqlite3";

	private static final String BAD_USER = "user_foo";

	@Configuration
	@ComponentScan(basePackageClasses = SpallocProperties.class)
	static class Config {
	}

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private SpallocAPI spalloc;

	@BeforeAll
	static void clearDB() throws IOException {
		Path dbp = Paths.get(DB);
		if (exists(dbp)) {
			log.info("deleting old database: {}", dbp);
			delete(dbp);
		}
	}

	@BeforeEach
	void checkSetup() {
		assumeTrue(db != null, "spring-configured DB engine absent");
		try (Connection c = db.getConnection()) {
			c.transaction(() -> setupDB1(c));
		}
	}

	private List<String> getReports() {
		return db.execute(c -> {
			try (Query q =
					c.query("SELECT reported_issue FROM board_reports")) {
				return q.call().map(string("reported_issue")).toList();
			}
		});
	}

	private void killReports() {
		db.executeVoid(c -> {
			try (Update u = c.update("DELETE from board_reports")) {
				u.call();
			}
		});
	}

	// Wrappers for temporarily putting the DB into a state with a job/alloc

	private void withJob(IntConsumer act) {
		int jobId = db.execute(c -> makeJob(c, null, QUEUED, null,
				ofEpochMilli(0), null, null, ofSeconds(0), now()));
		try {
			act.accept(jobId);
		} finally {
			nukeJob(jobId);
		}
	}

	private void nukeJob(int jobId) {
		db.executeVoid(c -> {
			try (Update u = c.update("DELETE FROM jobs WHERE job_id = ?")) {
				u.call(jobId);
			}
		});
	}

	private void withAllocation(int jobId, Runnable act) {
		db.executeVoid(c -> {
			allocateBoardToJob(c, BOARD, jobId);
			setAllocRoot(c, jobId, BOARD);
		});
		try {
			act.run();
		} finally {
			db.executeVoid(c -> {
				allocateBoardToJob(c, BOARD, null);
				setAllocRoot(c, jobId, null);
			});
		}
	}

	// The actual tests

	@Test
	public void getMachines() throws Exception {
		Map<String, Machine> machines = spalloc.getMachines(false);
		assertNotNull(machines);
		assertEquals(1, machines.size());
		assertEquals(asList(MACHINE_NAME), new ArrayList<>(machines.keySet()));
		// Not tagged
		Machine m = machines.get(MACHINE_NAME);
		assertEquals(MACHINE_NAME, m.getName());
		assertEquals(new HashSet<>(), m.getTags());
		assertEquals(asList(0), m.getAvailableBoards());
	}

	@Test
	public void listMachines() throws Exception {
		List<MachineListEntryRecord> machines = spalloc.listMachines(false);
		assertNotNull(machines);
		assertEquals(1, machines.size());
		MachineListEntryRecord machine = machines.get(0);
		assertEquals(MACHINE_NAME, machine.getName());
		// Not tagged
		assertEquals(asList(), machine.getTags());
		assertEquals(1, machine.getNumBoards());
	}

	@Test
	public void getMachine() throws Exception {
		Optional<Machine> m = spalloc.getMachine(MACHINE_NAME, false);
		assertNotNull(m);
		assertNotEquals(Optional.empty(), m);
		Machine machine = m.get();
		// Not tagged
		assertEquals(new HashSet<>(), machine.getTags());
		assertEquals(asList(0), machine.getAvailableBoards());
	}

	@Test
	public void getMachineInfo() throws Exception {
		inContext(c -> {
			Permit p = c.setAuth(USER_NAME);
			Optional<MachineDescription> m = spalloc
					.getMachineInfo(MACHINE_NAME, false, p);
			assertNotNull(m);
			assertNotEquals(Optional.empty(), m);
			MachineDescription machine = m.get();
			// Not tagged
			assertEquals(MACHINE_NAME, machine.getName());
			assertEquals(asList(), machine.getTags());
			assertEquals(asList(), machine.getLive()); // TODO fix test setup

			withJob(jobId -> withAllocation(jobId, () -> {
				MachineDescription m2 = spalloc.getMachineInfo(
						MACHINE_NAME, false, p).get();
				assertEquals(1, m2.getJobs().size());
				JobInfo j = m2.getJobs().get(0);
				assertEquals(Optional.of(USER_NAME), j.getOwner());
				assertEquals(jobId, j.getId());
				// URL not set; task of front-end only
				assertEquals(Optional.empty(), j.getUrl());
			}));
		});
	}

	@Test
	public void getJobs() {
		assertEquals(0, spalloc.getJobs(false, 10, 0).ids().size());

		// We don't run this after because withJob() leaves no trace
		assertEquals(0, spalloc.getJobs(true, 10, 0).ids().size());

		withJob(jobId -> {
			assertEquals(jobId, spalloc.getJobs(false, 10, 0).ids().get(0));
		});

		assertEquals(0, spalloc.getJobs(false, 10, 0).ids().size());
	}

	@Test
	public void listJobs() {
		inContext(c -> {
			Permit p = c.setAuth(USER_NAME);
			assertEquals(0, spalloc.listJobs(p).size());

			withJob(jobId -> {
				assertEquals(jobId, spalloc.listJobs(p).get(0).getId());
			});

			assertEquals(0, spalloc.listJobs(p).size());
		});
	}

	@Test
	public void getJob() {
		withJob(jobId -> inContext(c -> {
			// user_foo can't see user_bar's job details...
			Permit p0 = c.setAuth(BAD_USER);
			assertEquals(Optional.empty(), spalloc.getJob(p0, jobId));

			// ... but user_bar can.
			Permit p = c.setAuth(USER_NAME);
			assertNotEquals(Optional.empty(), spalloc.getJob(p, jobId));

			Job j = spalloc.getJob(p, jobId).get();

			assertEquals(jobId, j.getId());
			assertEquals(QUEUED, j.getState());
			assertEquals(USER_NAME, j.getOwner().get());
			// Not yet allocated so no machine to get
			assertEquals(Optional.empty(), j.getMachine());

			withAllocation(jobId, () -> {
				Job j2 = spalloc.getJob(p, jobId).get();

				assertEquals(jobId, j2.getId());
				assertEquals(QUEUED, j2.getState());
				assertEquals(USER_NAME, j2.getOwner().get());
				SubMachine m = j2.getMachine().get();
				assertEquals(MACHINE_NAME, m.getMachine().getName());
				assertEquals(1, m.getWidth());
				assertEquals(1, m.getHeight());
				assertEquals(1, m.getDepth());
				assertEquals(0, m.getRootX());
				assertEquals(0, m.getRootY());
				assertEquals(0, m.getRootZ());
				assertEquals(asList(new BoardCoordinates(0, 0, 0)),
						m.getBoards());
				assertEquals(OFF, m.getPower());
			});

			j.destroy("gorp");

			// Re-fetch to see state change
			assertEquals(QUEUED, j.getState());
			j = spalloc.getJob(p, jobId).get();
			assertEquals(DESTROYED, j.getState());
		}));
	}

	@Test
	public void getJobInfo() {
		withJob(jobId -> inContext(c -> {
			// user_foo can't see user_bar's job details...
			Permit p0 = c.setAuth(BAD_USER);
			assertEquals(Optional.empty(), spalloc.getJobInfo(p0, jobId));

			// ... but user_bar can.
			Permit p = c.setAuth(USER_NAME);
			assertNotEquals(Optional.empty(), spalloc.getJobInfo(p, jobId));

			JobDescription j = spalloc.getJobInfo(p, jobId).get();

			assertEquals(jobId, j.getId());
			assertEquals(QUEUED, j.getState());
			assertEquals(USER_NAME, j.getOwner().get());
			assertEquals(MACHINE_NAME, j.getMachine());

			withAllocation(jobId, () -> {
				JobDescription j2 = spalloc.getJobInfo(p, jobId).get();

				assertEquals(jobId, j2.getId());
				assertEquals(QUEUED, j2.getState());
				assertEquals(USER_NAME, j2.getOwner().get());
				assertEquals(MACHINE_NAME, j2.getMachine());
				assertEquals(Optional.of(8), j2.getWidth());
				assertEquals(Optional.of(8), j2.getHeight());
				assertEquals(
						asList(new BoardCoords(0, 0, 0, 1, 1, 0, "2.2.2.2")),
						j2.getBoards());
			});
		}));
	}

	@Test
	public void reportProblem() {
		inContext(c -> withJob(jobId -> withAllocation(jobId, () -> {
			try {
				assertEquals(asList(), getReports());

				Permit p = c.setAuth(USER_NAME);
				spalloc.reportProblem("2.2.2.2", null, "test", p);

				assertEquals(asList("test"), getReports());
			} finally {
				// Without this, we can't delete the job...
				killReports();
			}
		})));
	}

	@Test
	public void createJob() {
		Job job = spalloc
				.createJob(USER_NAME, GROUP_NAME, CreateBoard.triad(0, 0, 0),
						MACHINE_NAME, asList(), Duration.ofSeconds(1), null)
				.get();
		try {
			job.access("0.0.0.0");
			assertEquals(QUEUED, job.getState());
		} finally {
			nukeJob(job.getId());
		}

		job = spalloc
				.createJob(USER_NAME, GROUP_NAME, new CreateDimensions(1, 1, 1),
						MACHINE_NAME, asList(), Duration.ofSeconds(1), null)
				.get();
		try {
			assertEquals(QUEUED, job.getState());
		} finally {
			nukeJob(job.getId());
		}

		job = spalloc.createJob(USER_NAME, GROUP_NAME,
				new CreateDimensionsAt(1, 1, "2.2.2.2", null), MACHINE_NAME,
				asList(), Duration.ofSeconds(1), null).get();
		try {
			assertEquals(QUEUED, job.getState());
		} finally {
			nukeJob(job.getId());
		}

		job = spalloc
				.createJob(USER_NAME, GROUP_NAME, new CreateNumBoards(1, 0),
						MACHINE_NAME, asList(), Duration.ofSeconds(1), null)
				.get();
		try {
			assertEquals(QUEUED, job.getState());
		} finally {
			nukeJob(job.getId());
		}

		// Should be able to guess what group...
		job = spalloc
				.createJob(USER_NAME, null, new CreateNumBoards(1, 0),
						MACHINE_NAME, asList(), Duration.ofSeconds(1), null)
				.get();
		try {
			assertEquals(QUEUED, job.getState());
			// TODO check that the right group was selected
		} finally {
			nukeJob(job.getId());
		}
	}
}
