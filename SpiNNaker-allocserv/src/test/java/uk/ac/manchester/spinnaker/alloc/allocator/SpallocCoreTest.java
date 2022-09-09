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

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.OFF;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.TestSupport;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateBoard;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateDimensions;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateDimensionsAt;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateNumBoards;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.model.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.model.ConnectionInfo;
import uk.ac.manchester.spinnaker.alloc.web.IssueReportRequest;
import uk.ac.manchester.spinnaker.alloc.web.IssueReportRequest.ReportedBoard;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

@SpringBootTest
@SpringJUnitWebConfig(TestSupport.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + SpallocCoreTest.DB,
	"spalloc.historical-data.path=" + SpallocCoreTest.HIST_DB
})
class SpallocCoreTest extends TestSupport {
	/** The name of the database file. */
	static final String DB = "target/spalloc_test.sqlite3";

	/** The name of the database file. */
	static final String HIST_DB = "target/spalloc_test-hist.sqlite3";

	private static final String BAD_USER = "user_foo";

	@Autowired
	private SpallocAPI spalloc;

	@BeforeAll
	static void clearDB() throws IOException {
		killDB(DB);
	}

	@BeforeEach
	void checkSetup() {
		assumeTrue(db != null, "spring-configured DB engine absent");
		setupDB1();
	}

	// The actual tests

	@Test
	public void getMachines() throws Exception {
		var machines = spalloc.getMachines(false);
		assertNotNull(machines);
		assertEquals(1, machines.size());
		assertEquals(Set.of(MACHINE_NAME), machines.keySet());
		// Not tagged
		var m = machines.get(MACHINE_NAME);
		assertEquals(MACHINE_NAME, m.getName());
		assertEquals(Set.of(), m.getTags());
		assertEquals(List.of(0), m.getAvailableBoards());
	}

	@Test
	public void listMachines() throws Exception {
		var machines = spalloc.listMachines(false);
		assertNotNull(machines);
		assertEquals(1, machines.size());
		var machine = machines.get(0);
		assertEquals(MACHINE_NAME, machine.getName());
		// Not tagged
		assertEquals(List.of(), machine.getTags());
		assertEquals(1, machine.getNumBoards());
	}

	@Test
	public void getMachine() throws Exception {
		var m = spalloc.getMachine(MACHINE_NAME, false);
		assertNotNull(m);
		assertNotEquals(Optional.empty(), m);
		var machine = m.orElseThrow();
		// Not tagged
		assertEquals(Set.of(), machine.getTags());
		assertEquals(List.of(0), machine.getAvailableBoards());

		assertEquals(Optional.empty(),
				spalloc.getMachine("no such machine", false));
	}

	@Nested
	@DisplayName("Spalloc.Machine")
	class SpallocMachineTest {
		private Machine machine;

		@BeforeEach
		void getMachine() {
			machine = spalloc.getMachine(MACHINE_NAME, false).orElseThrow();
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
			assertEquals(Set.of(), machine.getTags());
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
			assertEquals(List.of(), machine.getDeadBoards());
		}

		@Test
		void getDownLinks() {
			assertEquals(List.of(), machine.getDownLinks());
		}

		@Test
		void getBoardByChip() {
			assertEquals(new BoardPhysicalCoordinates(1, 1, 0),
					machine.getBoardByChip(0, 0).orElseThrow().getPhysical());
		}

		@Test
		void getBoardByPhysicalCoords() {
			assertEquals(new BoardCoordinates(0, 0, 0),
					machine.getBoardByPhysicalCoords(1, 1, 0).orElseThrow()
							.getLogical());
		}

		@Test
		void getBoardByLogicalCoords() {
			assertEquals(new BoardPhysicalCoordinates(1, 1, 0),
					machine.getBoardByLogicalCoords(0, 0, 0).orElseThrow()
							.getPhysical());
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
			assertEquals(List.of(0), machine.getBoardNumbers());
		}

		@Test
		void getAvailableBoards() {
			assertEquals(List.of(0), machine.getAvailableBoards());
		}

		@Test
		void getBMPAddress() {
			assertEquals("1.1.1.1", machine.getBMPAddress(new BMPCoords(1, 1)));
		}

		@Test
		void getBoardNumbersOfBmp() {
			assertEquals(List.of(0),
					machine.getBoardNumbers(new BMPCoords(1, 1)));
		}
	}

	@Test
	public void getMachineInfo() throws Exception {
		inContext(c -> {
			var p = c.setAuth(USER_NAME);
			var m = spalloc.getMachineInfo(MACHINE_NAME, false, p);
			assertNotNull(m);
			assertNotEquals(Optional.empty(), m);
			var machine = m.orElseThrow();
			// Not tagged
			assertEquals(MACHINE_NAME, machine.getName());
			assertEquals(List.of(), machine.getTags());
			assertEquals(List.of(), machine.getLive()); // TODO fix test setup

			withJob(jobId -> withAllocation(jobId, () -> {
				var m2 = spalloc.getMachineInfo(MACHINE_NAME, false, p)
						.orElseThrow();
				assertEquals(1, m2.getJobs().size());
				var j = m2.getJobs().get(0);
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
				var md = spalloc.getMachineInfo(MACHINE_NAME, false, p)
						.orElseThrow();
				assertEquals(MACHINE, md.getId());
			});
		}

		@Test
		void getName() {
			withStandardAllocatedJob((p, jobId) -> {
				var md = spalloc.getMachineInfo(MACHINE_NAME, false, p)
						.orElseThrow();
				assertEquals(MACHINE_NAME, md.getName());
			});
		}

		@Test
		void getWidth() {
			withStandardAllocatedJob((p, jobId) -> {
				var md = spalloc.getMachineInfo(MACHINE_NAME, false, p)
						.orElseThrow();
				assertEquals(1, md.getWidth());
			});
		}

		@Test
		void getHeight() {
			withStandardAllocatedJob((p, jobId) -> {
				var md = spalloc.getMachineInfo(MACHINE_NAME, false, p)
						.orElseThrow();
				assertEquals(1, md.getHeight());
			});
		}

		@Test
		void getNumInUse() {
			withStandardAllocatedJob((p, jobId) -> {
				var md = spalloc.getMachineInfo(MACHINE_NAME, false, p)
						.orElseThrow();
				assertEquals(1, md.getNumInUse());
			});
		}

		@Test
		void getLive() {
			withStandardAllocatedJob((p, jobId) -> {
				var md = spalloc.getMachineInfo(MACHINE_NAME, false, p)
						.orElseThrow();
				assertEquals(List.of(), md.getLive());
			});
		}

		@Test
		void getDead() {
			withStandardAllocatedJob((p, jobId) -> {
				var md = spalloc.getMachineInfo(MACHINE_NAME, false, p)
						.orElseThrow();
				assertEquals(List.of(), md.getDead());
			});
		}

		@Test
		void getJobs() {
			withStandardAllocatedJob((p, jobId) -> {
				var md = spalloc.getMachineInfo(MACHINE_NAME, false, p)
						.orElseThrow();
				assertEquals(1, md.getJobs().size());
				var j = md.getJobs().get(0);
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
				var md = spalloc.getMachineInfo(MACHINE_NAME, false, p)
						.orElseThrow();
				assertEquals(List.of(), md.getTags());
			});
		}

		@Test
		void getQuota() {
			withStandardAllocatedJob((p, jobId) -> {
				var md = spalloc.getMachineInfo(MACHINE_NAME, false, p)
						.orElseThrow();
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
			var p = c.setAuth(USER_NAME);
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
			var p0 = c.setAuth(BAD_USER);
			assertEquals(Optional.empty(), spalloc.getJob(p0, jobId));

			// ... but user_bar can.
			var p = c.setAuth(USER_NAME);
			assertNotEquals(Optional.empty(), spalloc.getJob(p, jobId));

			var j = spalloc.getJob(p, jobId).orElseThrow();

			assertEquals(jobId, j.getId());
			assertEquals(QUEUED, j.getState());
			assertEquals(USER_NAME, j.getOwner().orElseThrow());
			// Not yet allocated so no machine to get
			assertEquals(Optional.empty(), j.getMachine());

			withAllocation(jobId, () -> {
				var j2 = spalloc.getJob(p, jobId).orElseThrow();

				assertEquals(jobId, j2.getId());
				assertEquals(QUEUED, j2.getState());
				assertEquals(USER_NAME, j2.getOwner().orElseThrow());
				var m = j2.getMachine().orElseThrow();
				assertEquals(MACHINE_NAME, m.getMachine().getName());
			});

			j.destroy("gorp");

			// Re-fetch to see state change
			assertEquals(QUEUED, j.getState());
			j = spalloc.getJob(p, jobId).orElseThrow();
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
				var j = spalloc.getJob(p, jobId).orElseThrow();
				assertEquals(jobId, j.getId());
			});
		}

		@Test
		void getState() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJob(p, jobId).orElseThrow();
				assertEquals(QUEUED, j.getState());
			});
		}

		@Test
		void getOwner() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJob(p, jobId).orElseThrow();
				assertEquals(Optional.of(USER_NAME), j.getOwner());
			});
		}

		@Test
		void getOriginalRequest() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJob(p, jobId).orElseThrow();
				// We didn't supply one
				assertEquals(Optional.empty(), j.getOriginalRequest());
			});
		}

		@Test
		void getWidth() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJob(p, jobId).orElseThrow();
				assertEquals(Optional.of(1), j.getWidth());
			});
		}

		@Test
		void getHeight() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJob(p, jobId).orElseThrow();
				assertEquals(Optional.of(1), j.getHeight());
			});
		}

		@Test
		void getDepth() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJob(p, jobId).orElseThrow();
				assertEquals(Optional.of(1), j.getDepth());
			});
		}

		@Test
		void getRootChip() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJob(p, jobId).orElseThrow();
				assertEquals(Optional.of(ZERO_ZERO), j.getRootChip());
			});
		}

		@Test
		void whereIs() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJob(p, jobId).orElseThrow();
				var bl = j.whereIs(4, 4).orElseThrow();
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
				var j = spalloc.getJob(p, jobId).orElseThrow();
				var sm = j.getMachine().orElseThrow();

				assertEquals(1, sm.getWidth());
				assertEquals(1, sm.getHeight());
				assertEquals(1, sm.getDepth());
				assertEquals(0, sm.getRootX());
				assertEquals(0, sm.getRootY());
				assertEquals(0, sm.getRootZ());

				assertEquals(List.of(new BoardCoordinates(0, 0, 0)),
						sm.getBoards());
				assertEquals(List.of(new ConnectionInfo(ZERO_ZERO, BOARD_ADDR)),
						sm.getConnections());

				assertEquals(MACHINE_NAME, sm.getMachine().getName());
				assertEquals(OFF, sm.getPower());
				sm.setPower(OFF);
				assertEquals(OFF, sm.getPower());
			});
		}

		@Test
		void keepalives() {
			var ts0 = Instant.now().truncatedTo(SECONDS);
			snooze1s();
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJob(p, jobId).orElseThrow();
				assertEquals(Optional.empty(), j.getKeepaliveHost());
				var ts1 = j.getKeepaliveTimestamp();
				assertTrue(ts0.isBefore(ts1));

				snooze1s();
				j.access("3.3.3.3");

				// reread
				var j2 = spalloc.getJob(p, jobId).orElseThrow();
				assertEquals(Optional.of("3.3.3.3"), j2.getKeepaliveHost());
				var ts2 = j2.getKeepaliveTimestamp();
				assertTrue(ts1.isBefore(ts2));
			});
		}

		@Test
		void getStartTime() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJob(p, jobId).orElseThrow();
				assertNotNull(j.getStartTime());
			});
		}

		@Test
		void termination() {
			// Don't hold an allocation for this
			inContext(c -> withJob(jobId -> {
				var p = c.setAuth(USER_NAME);

				var j = spalloc.getJob(p, jobId).orElseThrow();
				assertEquals(QUEUED, j.getState());
				assertEquals(Optional.empty(), j.getFinishTime());
				assertEquals(Optional.empty(), j.getReason());
				var ts0 = Instant.now().truncatedTo(SECONDS);
				snooze1s();

				j.destroy("foo bar");

				// reread
				var j2 = spalloc.getJob(p, jobId).orElseThrow();
				assertEquals(DESTROYED, j2.getState());
				var ts1 = j2.getFinishTime().orElseThrow();
				assertFalse(ts0.isAfter(ts1));
				assertEquals(Optional.of("foo bar"), j2.getReason());
			}));
		}

		@Test
		void reportIssue() {
			withStandardAllocatedJob((p, jobId) -> {
				try {
					assertEquals(List.of(), getReports());

					var j = spalloc.getJob(p, jobId).orElseThrow();
					// Messy to build as usually only done by Jackson
					var r = new IssueReportRequest();
					var b = new ReportedBoard();
					b.address = BOARD_ADDR;
					r.issue = "test";
					r.boards = List.of(b);
					j.reportIssue(r, p);

					assertEquals(List.of("test"), getReports());
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
			var p0 = c.setAuth(BAD_USER);
			assertEquals(Optional.empty(), spalloc.getJobInfo(p0, jobId));

			// ... but user_bar can.
			var p = c.setAuth(USER_NAME);
			assertNotEquals(Optional.empty(), spalloc.getJobInfo(p, jobId));

			var j = spalloc.getJobInfo(p, jobId).orElseThrow();

			assertEquals(jobId, j.getId());
			assertEquals(QUEUED, j.getState());
			assertEquals(USER_NAME, j.getOwner().orElseThrow());
		}));
		// See more detailed testing of JobDescription below
	}

	@Nested
	@DisplayName("Spalloc.JobDescription")
	class SpallocJobDescriptionTest {
		@Test
		void getId() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertEquals(jobId, j.getId());
			});
		}

		@Test
		void getOwner() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertEquals(Optional.of(USER_NAME), j.getOwner());
			});
		}

		@Test
		void getOwnerHost() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertEquals(Optional.empty(), j.getOwnerHost());
			});
		}

		@Test
		void getRequest() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertNull(j.getRequest());
			});
		}

		@Test
		void getRequestBytes() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertNull(j.getRequestBytes());
			});
		}

		@Test
		void getKeepAlive() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertEquals(Duration.ofSeconds(0), j.getKeepAlive());
			});
		}

		@Test
		void getState() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertEquals(QUEUED, j.getState());
			});
		}

		@Test
		void getHeight() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertEquals(Optional.of(8), j.getHeight());
			});
		}

		@Test
		void getWidth() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertEquals(Optional.of(8), j.getWidth());
			});
		}

		@Test
		void getTriadHeight() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertEquals(1, j.getTriadHeight());
			});
		}

		@Test
		void getTriadWidth() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertEquals(1, j.getTriadWidth());
			});
		}

		@Test
		void getMachine() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertEquals(MACHINE_NAME, j.getMachine());
			});
		}

		@Test
		void getMachineUrl() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertNull(j.getMachineUrl());
			});
		}

		@Test
		void getBoards() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertEquals(
						List.of(new BoardCoords(0, 0, 0, 1, 1, 0, BOARD_ADDR)),
						j.getBoards());
			});
		}

		@Test
		void isPowered() {
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertFalse(j.isPowered());
			});
		}

		@Test
		void getStartTime() {
			var ts0 = Instant.now().truncatedTo(SECONDS);
			snooze1s();
			withStandardAllocatedJob((p, jobId) -> {
				var j = spalloc.getJobInfo(p, jobId).orElseThrow();
				assertTrue(ts0.isBefore(j.getStartTime()));
			});
		}
	}

	@Test
	public void reportProblem() {
		inContext(c -> withJob(jobId -> withAllocation(jobId, () -> {
			try {
				assertEquals(List.of(), getReports());

				var p = c.setAuth(USER_NAME);
				spalloc.reportProblem(BOARD_ADDR, null, "test", p);

				assertEquals(List.of("test"), getReports());
			} finally {
				// Without this, we can't delete the job...
				killReports();
			}
		})));
	}

	@Test
	public void createJob() {
		var job = spalloc
				.createJob(USER_NAME, GROUP_NAME, CreateBoard.triad(0, 0, 0),
						MACHINE_NAME, List.of(), Duration.ofSeconds(1), null)
				.orElseThrow();
		try {
			job.access("0.0.0.0");
			assertEquals(QUEUED, job.getState());
		} finally {
			nukeJob(job.getId());
		}

		job = spalloc
				.createJob(USER_NAME, GROUP_NAME, new CreateDimensions(1, 1, 1),
						MACHINE_NAME, List.of(), Duration.ofSeconds(1), null)
				.orElseThrow();
		try {
			assertEquals(QUEUED, job.getState());
		} finally {
			nukeJob(job.getId());
		}

		job = spalloc.createJob(USER_NAME, GROUP_NAME,
				new CreateDimensionsAt(1, 1, BOARD_ADDR, null), MACHINE_NAME,
				List.of(), Duration.ofSeconds(1), null).orElseThrow();
		try {
			assertEquals(QUEUED, job.getState());
		} finally {
			nukeJob(job.getId());
		}

		job = spalloc
				.createJob(USER_NAME, GROUP_NAME, new CreateNumBoards(1, 0),
						MACHINE_NAME, List.of(), Duration.ofSeconds(1), null)
				.orElseThrow();
		try {
			assertEquals(QUEUED, job.getState());
		} finally {
			nukeJob(job.getId());
		}

		// Should be able to guess what group...
		job = spalloc
				.createJob(USER_NAME, null, new CreateNumBoards(1, 0),
						MACHINE_NAME, List.of(), Duration.ofSeconds(1), null)
				.orElseThrow();
		try {
			assertEquals(QUEUED, job.getState());
			// TODO check that the right group was selected
		} finally {
			nukeJob(job.getId());
		}
	}
}
