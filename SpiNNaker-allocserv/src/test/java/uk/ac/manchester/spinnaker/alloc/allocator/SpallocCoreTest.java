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
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.MACHINE_NAME;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.USER_NAME;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.makeJob;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.setupDB1;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityUtils.inContext;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.model.JobDescription;
import uk.ac.manchester.spinnaker.alloc.model.MachineDescription;
import uk.ac.manchester.spinnaker.alloc.model.MachineListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.security.Permit;

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

	//private TestAPI testAPI;

	@BeforeAll
	static void clearDB() throws IOException {
		Path dbp = Paths.get(DB);
		if (exists(dbp)) {
			log.info("deleting old database: {}", dbp);
			delete(dbp);
		}
	}

	@BeforeEach
	//@SuppressWarnings("deprecation")
	void checkSetup() {
		assumeTrue(db != null, "spring-configured DB engine absent");
		try (Connection c = db.getConnection()) {
			c.transaction(() -> setupDB1(c));
		}
		//testAPI = spalloc.getTestAPI();
	}

	private interface WithJob {
		void act(int jobId);
	}

	private void withJob(WithJob act) {
		int jobId = db.execute(c -> makeJob(c, null, QUEUED, null,
				ofEpochMilli(0), null, null, ofSeconds(0), now()));
		try {
			act.act(jobId);
		} finally {
			db.executeVoid(c1 -> {
				try (Update u =
						c1.update("DELETE FROM jobs WHERE job_id = ?")) {
					u.call(jobId);
				}
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
	public void getMachineByName() throws Exception {
		Optional<Machine> m = spalloc.getMachine(MACHINE_NAME, false);
		assertTrue(m.isPresent());
		Machine machine = m.get();
		// Not tagged
		assertEquals(new HashSet<>(), machine.getTags());
		assertEquals(asList(0), machine.getAvailableBoards());
	}

	@Test
	public void getMachineInfo() throws Exception {
		inContext(c -> {
			SecurityContext context = SecurityContextHolder.getContext();

			c.setAuth(USER_NAME);
			Optional<MachineDescription> m = spalloc
					.getMachineInfo(MACHINE_NAME, false, new Permit(context));
			assertTrue(m.isPresent());
			MachineDescription machine = m.get();
			// Not tagged
			assertEquals(MACHINE_NAME, machine.getName());
			assertEquals(asList(), machine.getTags());
			assertEquals(asList(), machine.getLive()); // TODO fix test setup
		});
	}

	@Test
	public void getJobs() {
		assertEquals(0, spalloc.getJobs(false, 10, 0).ids().size());

		withJob(jobId -> {
			assertEquals(jobId, spalloc.getJobs(false, 10, 0).ids().get(0));
		});

		assertEquals(0, spalloc.getJobs(false, 10, 0).ids().size());
	}

	@Test
	public void getJob() {
		withJob(jobId -> inContext(c -> {
			SecurityContext context = SecurityContextHolder.getContext();

			// user_foo can't see user_bar's job details...
			c.setAuth(BAD_USER);
			assertFalse(spalloc.getJob(new Permit(context), jobId).isPresent());

			// ... but user_bar can.
			c.setAuth(USER_NAME);
			Permit p = new Permit(context);
			assertTrue(spalloc.getJob(p, jobId).isPresent());

			Job j = spalloc.getJob(p, jobId).get();

			assertEquals(jobId, j.getId());
			assertEquals(QUEUED, j.getState());
			assertEquals(USER_NAME, j.getOwner().get());
			// Not yet allocated so no machine to get
			assertFalse(j.getMachine().isPresent());

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
			SecurityContext context = SecurityContextHolder.getContext();

			// user_foo can't see user_bar's job details...
			c.setAuth(BAD_USER);
			assertFalse(
					spalloc.getJobInfo(new Permit(context), jobId).isPresent());

			// ... but user_bar can.
			c.setAuth(USER_NAME);
			Permit p = new Permit(context);
			assertTrue(spalloc.getJobInfo(p, jobId).isPresent());

			JobDescription j = spalloc.getJobInfo(p, jobId).get();

			assertEquals(jobId, j.getId());
			assertEquals(QUEUED, j.getState());
			assertEquals(USER_NAME, j.getOwner().get());
		}));
	}
}
