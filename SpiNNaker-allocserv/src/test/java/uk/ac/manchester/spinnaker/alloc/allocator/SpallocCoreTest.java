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
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.makeJob;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.setupDB1;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

	private interface C {
		void setAuth(Authentication a);

		@SuppressWarnings("serial")
		default void setAuth(String name) {
			setAuth(new Authentication() {
				@Override
				public String getName() {
					return name;
				}

				@Override
				public Collection<? extends GrantedAuthority> getAuthorities() {
					return new ArrayList<>();
				}

				@Override
				public Object getCredentials() {
					return null;
				}

				@Override
				public Object getDetails() {
					return null;
				}

				@Override
				public Object getPrincipal() {
					return null;
				}

				@Override
				public boolean isAuthenticated() {
					return true;
				}

				@Override
				public void setAuthenticated(boolean isAuthenticated) {
				}
			});
		}
	}

	private interface InC {
		void act(C c);
	}

	private void inContext(InC inc) {
		SecurityContext context = SecurityContextHolder.getContext();
		C c = context::setAuthentication;
		try {
			inc.act(c);
		} finally {
			context.setAuthentication(null);
		}
	}

	// The actual tests

	@Test
	public void getMachines() throws Exception {
		Map<String, Machine> machines = spalloc.getMachines(false);
		assertNotNull(machines);
		assertEquals(1, machines.size());
		assertEquals(asList("foo"), new ArrayList<>(machines.keySet()));
		// Not tagged
		assertEquals(new HashSet<>(), machines.get("foo").getTags());
		assertEquals(asList(0), machines.get("foo").getAvailableBoards());
	}

	@Test
	public void getMachineByName() throws Exception {
		Optional<Machine> m = spalloc.getMachine("foo", false);
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

			c.setAuth("foo");
			Optional<MachineDescription> m =
					spalloc.getMachineInfo("foo", false, new Permit(context));
			assertTrue(m.isPresent());
			MachineDescription machine = m.get();
			// Not tagged
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

			// foo can't see bar's job details...
			c.setAuth("foo");
			assertFalse(spalloc.getJob(new Permit(context), jobId).isPresent());

			// ... but bar can.
			c.setAuth("bar");
			Permit p = new Permit(context);
			assertTrue(spalloc.getJob(p, jobId).isPresent());

			Job j = spalloc.getJob(p, jobId).get();

			assertEquals(jobId, j.getId());
			assertEquals(QUEUED, j.getState());
			assertEquals("bar", j.getOwner().get());

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

			// foo can't see bar's job details...
			c.setAuth("foo");
			assertFalse(
					spalloc.getJobInfo(new Permit(context), jobId).isPresent());

			// ... but bar can.
			c.setAuth("bar");
			Permit p = new Permit(context);
			assertTrue(spalloc.getJobInfo(p, jobId).isPresent());

			JobDescription j = spalloc.getJobInfo(p, jobId).get();

			assertEquals(jobId, j.getId());
			assertEquals(QUEUED, j.getState());
			assertEquals("bar", j.getOwner().get());
		}));
	}
}
