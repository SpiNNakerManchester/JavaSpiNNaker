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
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.BOARD;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.BOARD_ADDR;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.GROUP_NAME;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.MACHINE;
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
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.ObjIntConsumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
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
import uk.ac.manchester.spinnaker.alloc.model.ConnectionInfo;
import uk.ac.manchester.spinnaker.alloc.model.JobDescription;
import uk.ac.manchester.spinnaker.alloc.model.MachineDescription;
import uk.ac.manchester.spinnaker.alloc.model.MachineDescription.JobInfo;
import uk.ac.manchester.spinnaker.alloc.model.MachineListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.security.Permit;
import uk.ac.manchester.spinnaker.alloc.web.IssueReportRequest;
import uk.ac.manchester.spinnaker.alloc.web.IssueReportRequest.ReportedBoard;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

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

	private void withStandardAllocatedJob(ObjIntConsumer<Permit> act) {
		// Composite op, for brevity
		withJob(jobId -> inContext(c -> withAllocation(jobId,
				() -> act.accept(c.setAuth(USER_NAME), jobId))));
	}

	private static final int DELAY_MS = 1000;

	private static void snooze() {
		try {
			Thread.sleep(DELAY_MS);
		} catch (InterruptedException e) {
			assumeTrue(false, "sleep() was interrupted");
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

		assertEquals(Optional.empty(),
				spalloc.getMachine("no such machine", false));
	}

	@Nested
	@DisplayName("Spalloc.Machine")
	class SpallocMachineTest {
		private Machine machine;

		@BeforeEach
		void getMachine() {
			machine = spalloc.getMachine(MACHINE_NAME, false).get();
		}

		@Test
		void getId() {
			assertEquals(MACHINE, machine.getId());
		}

		@Test
		void getName() {
			assertEquals(MACHINE_NAME, machine.getName());
		}

		@Test
		void getTags() {
			assertEquals(new HashSet<>(), machine.getTags());
		}

		@Test
		void getWidth() {
			assertEquals(1, machine.getWidth());
		}

		@Test
		void getHeight() {
			assertEquals(1, machine.getHeight());
		}

		@Test
		void isInService() {
			assertTrue(machine.isInService());
		}

		@Test
		void getDeadBoards() {
			assertEquals(asList(), machine.getDeadBoards());
		}

		@Test
		void getDownLinks() {
			assertEquals(asList(), machine.getDownLinks());
		}

		@Test
		void getBoardByChip() {
			assertEquals(new BoardPhysicalCoordinates(1, 1, 0),
					machine.getBoardByChip(0, 0).get().getPhysical());
		}

		@Test
		void getBoardByPhysicalCoords() {
			assertEquals(new BoardCoordinates(0, 0, 0), machine
					.getBoardByPhysicalCoords(1, 1, 0).get().getLogical());
		}

		@Test
		void getBoardByLogicalCoords() {
			assertEquals(new BoardPhysicalCoordinates(1, 1, 0), machine
					.getBoardByLogicalCoords(0, 0, 0).get().getPhysical());
		}

		@Test
		void getBoardByIPAddress() {
			assertEquals(Optional.empty(), machine.getBoardByIPAddress(""));
		}

		@Test
		void getRootBoardBMPAddress() {
			assertEquals("1.1.1.1", machine.getRootBoardBMPAddress());
		}

		@Test
		void getBoardNumbers() {
			assertEquals(asList(0), machine.getBoardNumbers());
		}

		@Test
		void getAvailableBoards() {
			assertEquals(asList(0), machine.getAvailableBoards());
		}

		@Test
		void getBMPAddress() {
			assertEquals("1.1.1.1", machine.getBMPAddress(new BMPCoords(1, 1)));
		}

		@Test
		void getBoardNumbersOfBmp() {
			assertEquals(asList(0),
					machine.getBoardNumbers(new BMPCoords(1, 1)));
		}
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

			assertEquals(Optional.empty(),
					spalloc.getMachineInfo("no such machine", false, p));
		});
	}

	@Nested
	@DisplayName("Spalloc.MachineDescription")
	class SpallocMachineDescriptionTest {
		@Test
		void getId() {
			withStandardAllocatedJob((p, jobId) -> {
				MachineDescription md =
						spalloc.getMachineInfo(MACHINE_NAME, false, p).get();
				assertEquals(MACHINE, md.getId());
			});
		}

		@Test
		void getName() {
			withStandardAllocatedJob((p, jobId) -> {
				MachineDescription md =
						spalloc.getMachineInfo(MACHINE_NAME, false, p).get();
				assertEquals(MACHINE_NAME, md.getName());
			});
		}

		@Test
		void getWidth() {
			withStandardAllocatedJob((p, jobId) -> {
				MachineDescription md =
						spalloc.getMachineInfo(MACHINE_NAME, false, p).get();
				assertEquals(1, md.getWidth());
			});
		}

		@Test
		void getHeight() {
			withStandardAllocatedJob((p, jobId) -> {
				MachineDescription md =
						spalloc.getMachineInfo(MACHINE_NAME, false, p).get();
				assertEquals(1, md.getHeight());
			});
		}

		@Test
		void getNumInUse() {
			withStandardAllocatedJob((p, jobId) -> {
				MachineDescription md =
						spalloc.getMachineInfo(MACHINE_NAME, false, p).get();
				assertEquals(1, md.getNumInUse());
			});
		}

		@Test
		void getLive() {
			withStandardAllocatedJob((p, jobId) -> {
				MachineDescription md =
						spalloc.getMachineInfo(MACHINE_NAME, false, p).get();
				assertEquals(asList(), md.getLive());
			});
		}

		@Test
		void getDead() {
			withStandardAllocatedJob((p, jobId) -> {
				MachineDescription md =
						spalloc.getMachineInfo(MACHINE_NAME, false, p).get();
				assertEquals(asList(), md.getDead());
			});
		}

		@Test
		void getJobs() {
			withStandardAllocatedJob((p, jobId) -> {
				MachineDescription md =
						spalloc.getMachineInfo(MACHINE_NAME, false, p).get();
				assertEquals(1, md.getJobs().size());
				JobInfo j = md.getJobs().get(0);
				assertEquals(jobId, j.getId());
				assertEquals(Optional.of(USER_NAME), j.getOwner());
				assertEquals(Optional.empty(), j.getUrl());
				assertEquals(1, j.getBoards().size());
				assertEquals(BOARD_ADDR, j.getBoards().get(0).getAddress());
			});
		}

		@Test
		void getTags() {
			withStandardAllocatedJob((p, jobId) -> {
				MachineDescription md =
						spalloc.getMachineInfo(MACHINE_NAME, false, p).get();
				assertEquals(asList(), md.getTags());
			});
		}

		@Test
		void getQuota() {
			withStandardAllocatedJob((p, jobId) -> {
				MachineDescription md =
						spalloc.getMachineInfo(MACHINE_NAME, false, p).get();
				assertEquals(Optional.of(1024L), md.getQuota());
			});
		}
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
			});

			j.destroy("gorp");

			// Re-fetch to see state change
			assertEquals(QUEUED, j.getState());
			j = spalloc.getJob(p, jobId).get();
			assertEquals(DESTROYED, j.getState());
		}));
		// See more detailed testing of Job below
	}

	@Nested
	@DisplayName("Spalloc.Job")
	class SpallocJobTest {
		@Test
		void getId() {
			withStandardAllocatedJob((p, jobId) -> {
				Job j = spalloc.getJob(p, jobId).get();
				assertEquals(jobId, j.getId());
			});
		}

		@Test
		void getState() {
			withStandardAllocatedJob((p, jobId) -> {
				Job j = spalloc.getJob(p, jobId).get();
				assertEquals(QUEUED, j.getState());
			});
		}

		@Test
		void getOwner() {
			withStandardAllocatedJob((p, jobId) -> {
				Job j = spalloc.getJob(p, jobId).get();
				assertEquals(Optional.of(USER_NAME), j.getOwner());
			});
		}

		@Test
		void getOriginalRequest() {
			withStandardAllocatedJob((p, jobId) -> {
				Job j = spalloc.getJob(p, jobId).get();
				// We didn't supply one
				assertEquals(Optional.empty(), j.getOriginalRequest());
			});
		}

		@Test
		void getWidth() {
			withStandardAllocatedJob((p, jobId) -> {
				Job j = spalloc.getJob(p, jobId).get();
				assertEquals(Optional.of(1), j.getWidth());
			});
		}

		@Test
		void getHeight() {
			withStandardAllocatedJob((p, jobId) -> {
				Job j = spalloc.getJob(p, jobId).get();
				assertEquals(Optional.of(1), j.getHeight());
			});
		}

		@Test
		void getDepth() {
			withStandardAllocatedJob((p, jobId) -> {
				Job j = spalloc.getJob(p, jobId).get();
				assertEquals(Optional.of(1), j.getDepth());
			});
		}

		@Test
		void getRootChip() {
			withStandardAllocatedJob((p, jobId) -> {
				Job j = spalloc.getJob(p, jobId).get();
				assertEquals(Optional.of(ZERO_ZERO), j.getRootChip());
			});
		}

		@Test
		void whereIs() {
			withStandardAllocatedJob((p, jobId) -> {
				Job j = spalloc.getJob(p, jobId).get();
				BoardLocation bl = j.whereIs(4, 4).get();
				assertEquals(ZERO_ZERO, bl.getBoardChip());
				assertEquals(new ChipLocation(4, 4), bl.getChip());
				assertEquals(new ChipLocation(1, 3),
						bl.getChipRelativeTo(new ChipLocation(3, 1)));
				assertEquals(jobId, bl.getJob().getId());
				assertEquals(new BoardCoordinates(0, 0, 0), bl.getLogical());
				assertEquals(MACHINE_NAME, bl.getMachine());
				assertEquals(new BoardPhysicalCoordinates(1, 1, 0),
						bl.getPhysical());
			});
		}

		@Test
		void getMachine() {
			withStandardAllocatedJob((p, jobId) -> {
				Job j = spalloc.getJob(p, jobId).get();
				SubMachine sm = j.getMachine().get();

				assertEquals(1, sm.getWidth());
				assertEquals(1, sm.getHeight());
				assertEquals(1, sm.getDepth());
				assertEquals(0, sm.getRootX());
				assertEquals(0, sm.getRootY());
				assertEquals(0, sm.getRootZ());

				assertEquals(asList(new BoardCoordinates(0, 0, 0)),
						sm.getBoards());
				assertEquals(asList(new ConnectionInfo(ZERO_ZERO, BOARD_ADDR)),
						sm.getConnections());

				assertEquals(MACHINE_NAME, sm.getMachine().getName());
				assertEquals(OFF, sm.getPower());
				sm.setPower(OFF);
				assertEquals(OFF, sm.getPower());
			});
		}

		@Test
		void keepalives() {
			Instant ts0 = Instant.now().truncatedTo(SECONDS);
			snooze();
			withStandardAllocatedJob((p, jobId) -> {
				Job j = spalloc.getJob(p, jobId).get();
				assertEquals(Optional.empty(), j.getKeepaliveHost());
				Instant ts1 = j.getKeepaliveTimestamp();
				assertTrue(ts0.isBefore(ts1));

				snooze();
				j.access("3.3.3.3");

				// reread
				Job j2 = spalloc.getJob(p, jobId).get();
				assertEquals(Optional.of("3.3.3.3"), j2.getKeepaliveHost());
				Instant ts2 = j2.getKeepaliveTimestamp();
				assertTrue(ts1.isBefore(ts2));
			});
		}

		@Test
		void getStartTime() {
			withStandardAllocatedJob((p, jobId) -> {
				Job j = spalloc.getJob(p, jobId).get();
				assertNotNull(j.getStartTime());
			});
		}

		@Test
		void termination() {
			// Don't hold an allocation for this
			inContext(c -> withJob(jobId -> {
				Permit p = c.setAuth(USER_NAME);

				Job j = spalloc.getJob(p, jobId).get();
				assertEquals(QUEUED, j.getState());
				assertEquals(Optional.empty(), j.getFinishTime());
				assertEquals(Optional.empty(), j.getReason());
				Instant ts0 = Instant.now().truncatedTo(SECONDS);

				j.destroy("foo bar");

				// reread
				Job j2 = spalloc.getJob(p, jobId).get();
				assertEquals(DESTROYED, j2.getState());
				Instant ts1 = j2.getFinishTime().get();
				assertFalse(ts0.isAfter(ts1));
				assertEquals(Optional.of("foo bar"), j2.getReason());
			}));
		}

		@Test
		void reportIssue() {
			withStandardAllocatedJob((p, jobId) -> {
				try {
					assertEquals(asList(), getReports());

					Job j = spalloc.getJob(p, jobId).get();
					// Messy to build as usually only done by Jackson
					IssueReportRequest r = new IssueReportRequest();
					ReportedBoard b = new ReportedBoard();
					b.address = BOARD_ADDR;
					r.issue = "test";
					r.boards = asList(b);
					j.reportIssue(r, p);

					assertEquals(asList("test"), getReports());
				} finally {
					// Without this, we can't delete the job...
					killReports();
				}
			});
		}
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
		}));
		// See more detailed testing of JobDescription below
	}

	@Nested
	@DisplayName("Spalloc.JobDescription")
	class SpallocJobDescriptionTest {
		@Test
		void getId() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertEquals(jobId, j.getId());
			});
		}

		@Test
		void getOwner() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertEquals(Optional.of(USER_NAME), j.getOwner());
			});
		}

		@Test
		void getOwnerHost() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertEquals(Optional.empty(), j.getOwnerHost());
			});
		}

		@Test
		void getRequest() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertNull(j.getRequest());
			});
		}

		@Test
		void getRequestBytes() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertNull(j.getRequestBytes());
			});
		}

		@Test
		void getKeepAlive() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertEquals(Duration.ofSeconds(0), j.getKeepAlive());
			});
		}

		@Test
		void getState() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertEquals(QUEUED, j.getState());
			});
		}

		@Test
		void getHeight() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertEquals(Optional.of(8), j.getHeight());
			});
		}

		@Test
		void getWidth() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertEquals(Optional.of(8), j.getWidth());
			});
		}

		@Test
		void getTriadHeight() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertEquals(1, j.getTriadHeight());
			});
		}

		@Test
		void getTriadWidth() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertEquals(1, j.getTriadWidth());
			});
		}

		@Test
		void getMachine() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertEquals(MACHINE_NAME, j.getMachine());
			});
		}

		@Test
		void getMachineUrl() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertNull(j.getMachineUrl());
			});
		}

		@Test
		void getBoards() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertEquals(
						asList(new BoardCoords(0, 0, 0, 1, 1, 0, BOARD_ADDR)),
						j.getBoards());
			});
		}

		@Test
		void isPowered() {
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertFalse(j.isPowered());
			});
		}

		@Test
		void getStartTime() {
			Instant ts0 = Instant.now().truncatedTo(SECONDS);
			snooze();
			withStandardAllocatedJob((p, jobId) -> {
				JobDescription j = spalloc.getJobInfo(p, jobId).get();
				assertTrue(ts0.isBefore(j.getStartTime()));
			});
		}
	}

	@Test
	public void reportProblem() {
		inContext(c -> withJob(jobId -> withAllocation(jobId, () -> {
			try {
				assertEquals(asList(), getReports());

				Permit p = c.setAuth(USER_NAME);
				spalloc.reportProblem(BOARD_ADDR, null, "test", p);

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
				new CreateDimensionsAt(1, 1, BOARD_ADDR, null), MACHINE_NAME,
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
