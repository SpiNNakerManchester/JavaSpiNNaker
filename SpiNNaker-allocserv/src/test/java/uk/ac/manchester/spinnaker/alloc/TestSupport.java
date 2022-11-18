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
package uk.ac.manchester.spinnaker.alloc;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.time.Instant.ofEpochSecond;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;
import static uk.ac.manchester.spinnaker.alloc.security.TrustLevel.BASIC;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.ObjIntConsumer;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connected;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Transacted;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.security.Permit;

@SuppressWarnings({
	"checkstyle:ParameterNumber", "checkstyle:VisibilityModifier"
})
public abstract class TestSupport extends SQLQueries implements SupportQueries {
	/** Bring in the application Spring configuration that we're testing. */
	@Configuration
	@ComponentScan(basePackageClasses = SpallocProperties.class)
	public static class Config {
	}

	protected static final Logger log = getLogger(TestSupport.class);

	/** Machine ID. */
	protected static final int MACHINE = 1000;

	/** Machine name. */
	protected static final String MACHINE_NAME = "foo_machine";

	/** BMP ID. */
	protected static final int BMP = 2000;

	/** BMP IP address. */
	protected static final String BMP_ADDR = "1.1.1.1";

	/** Board ID. */
	protected static final int BOARD = 3000;

	/** Board IP address. */
	protected static final String BOARD_ADDR = "2.2.2.2";

	/** User ID. */
	protected static final int USER = 4000;

	/** User name. */
	public static final String USER_NAME = "user_bar";

	/** Group ID. */
	protected static final int GROUP = 5000;

	/** Group name. */
	public static final String GROUP_NAME = "grill";

	/** User-group membership ID. */
	protected static final int MEMBERSHIP = 6000;

	/** Initial quota. */
	protected static final long INITIAL_QUOTA = 1024;

	/** The DB. */
	@Autowired
	protected DatabaseEngine db;

	/** The context connection to the DB. */
	protected Connection conn;

	/**
	 * Delete a DB file. It must not be open!
	 *
	 * @param dbPath
	 *            Where the DB is.
	 * @throws IOException
	 *             On failure.
	 */
	protected static void killDB(String dbPath) throws IOException {
		var dbp = Paths.get(dbPath);
		if (exists(dbp)) {
			log.info("deleting old database: {}", dbp);
			delete(dbp);
		}
	}

	private static void makeMachine(Connection c, int width, int height,
			int depth) {
		try (var u = c.update(INSERT_MACHINE)) {
			u.call(MACHINE, MACHINE_NAME, width, height, depth);
		}
		try (var u = c.update(INSERT_BMP_WITH_ID)) {
			u.call(BMP, MACHINE, BMP_ADDR, 1, 1);
		}
	}

	private static void makeUser(Connection c) {
		try (var u = c.update(INSERT_USER)) {
			u.call(USER, USER_NAME, BASIC, true);
		}
		try (var u = c.update(INSERT_GROUP)) {
			u.call(GROUP, GROUP_NAME, INITIAL_QUOTA);
		}
		try (var u = c.update(INSERT_MEMBER)) {
			u.call(MEMBERSHIP, USER, GROUP);
		}
	}

	/**
	 * Set up a machine with one board, and a user.
	 */
	protected void setupDB1() {
		try (var c = db.getConnection()) {
			c.transaction(() -> setupDB1(c));
		}
	}

	private static void setupDB1(Connection c) {
		// A simple machine
		makeMachine(c, 1, 1, 1);
		// Add one board to the machine
		try (var u = c.update(INSERT_BOARD_WITH_ID)) {
			u.call(BOARD, BOARD_ADDR, BMP, 0, MACHINE, 0, 0, 0, 0, 0, false);
		}
		// A disabled permission-less user with a quota
		makeUser(c);
	}

	/**
	 * Set up a machine with three boards, and a user.
	 */
	protected void setupDB3() {
		try (var c = db.getConnection()) {
			c.transaction(() -> setupDB3(c));
		}
	}

	private static void setupDB3(Connection c) {
		// A simple machine
		makeMachine(c, 1, 1, 3);
		// Add three connected boards to the machine
		int b0 = BOARD, b1 = BOARD + 1, b2 = BOARD + 2;
		try (var u = c.update(INSERT_BOARD_WITH_ID)) {
			u.call(b0, BOARD_ADDR, BMP, 0, MACHINE, 0, 0, 0, 0, 0, false);
			u.call(b1, "2.2.2.3", BMP, 1, MACHINE, 0, 0, 1, 8, 4, false);
			u.call(b2, "2.2.2.4", BMP, 2, MACHINE, 0, 0, 2, 4, 8, false);
		}
		try (var u = c.update(INSERT_LINK)) {
			u.call(b0, 0, b1, 3, true);
			u.call(b0, 1, b2, 4, true);
			u.call(b1, 2, b2, 5, true);
		}
		// A disabled permission-less user with a quota
		makeUser(c);
	}

	/**
	 * Insert a job of a given size and length.
	 *
	 * @param c
	 *            DB connection
	 * @param root
	 *            Root board, or {@code null}
	 * @param state
	 *            Job state
	 * @param size
	 *            Number of boards, or {@code null}
	 * @param createTime
	 *            Time of creation, or {@code null}
	 * @param allocateTime
	 *            Time of allocation, or {@code null}
	 * @param deathTime
	 *            Time of death, or {@code null}
	 * @param keepalive
	 *            Keepalive interval, or {@code null}
	 * @param keepaliveTime
	 *            Time of last keepalive, or {@code null}
	 * @return Job ID
	 */
	protected static int makeJob(Connection c, Integer root,
			@NonNull JobState state, Integer size, Instant createTime,
			Instant allocateTime, Instant deathTime, Duration keepalive,
			Instant keepaliveTime) {
		try (var u = c.update(INSERT_JOB_WITH_TIMESTAMPS)) {
			return u.key(MACHINE, USER, GROUP, root, state, createTime,
					allocateTime, deathTime, size, keepalive, keepaliveTime)
					.orElseThrow(
							() -> new RuntimeException("failed to insert job"));
		}
	}

	/**
	 * Insert a dead job of a given size and length.
	 *
	 * @param c
	 *            DB connection
	 * @param size
	 *            Number of boards
	 * @param time
	 *            Length of time (seconds)
	 * @return Job ID
	 */
	protected static int makeFinishedJob(Connection c, int size, int time) {
		return makeJob(c, BOARD, DESTROYED, size, ofEpochSecond(0),
				ofEpochSecond(time), ofEpochSecond(time + time),
				ofSeconds(time), null);
	}

	/**
	 * Directly manipulate the allocation.
	 *
	 * @param c
	 *            DB connection
	 * @param boardId
	 *            What board are we changing
	 * @param jobId
	 *            What job are we assigning
	 */
	protected static void allocateBoardToJob(Connection c, int boardId,
			Integer jobId) {
		try (var u = c.update("UPDATE boards SET allocated_job = :job "
				+ "WHERE board_id = :board")) {
			u.call(jobId, boardId);
		}
	}

	/**
	 * Directly manipulate the reverse of the allocation (root only).
	 *
	 * @param c
	 *            DB connection
	 * @param jobId
	 *            What job are we changing
	 * @param boardId
	 *            What board are we assigning as the root
	 */
	protected static void setAllocRoot(Connection c, int jobId,
			Integer boardId) {
		try (var u = c.update("UPDATE jobs SET root_id = :board, "
				+ "width = 1, height = 1, depth = 1 "
				+ "WHERE job_id = :job")) {
			u.call(boardId, jobId);
		}
	}

	protected List<String> getReports() {
		return db.execute(c -> {
			try (var q = c.query("SELECT reported_issue FROM board_reports")) {
				return q.call().map(string("reported_issue")).toList();
			}
		});
	}

	protected void killReports() {
		db.executeVoid(c -> {
			try (var u = c.update("DELETE from board_reports")) {
				u.call();
			}
		});
	}

	protected void checkAndRollback(Connected act) {
		db.executeVoid(c -> {
			try {
				conn = c;
				act.act(c);
			} finally {
				c.rollback();
			}
		});
	}

	protected void doTransactionalTest(Transacted action) {
		try (var c = db.getConnection()) {
			c.transaction(() -> {
				try {
					conn = c;
					action.act();
				} finally {
					try {
						c.rollback();
					} catch (DataAccessException ignored) {
						log.trace("ignoring DAE from rollback", ignored);
					}
					conn = null;
				}
			});
		}
	}

	/**
	 * Insert a live job. Needs a matching allocation request.
	 *
	 * @param time
	 *            Length of time for keepalive (seconds)
	 * @return Job ID
	 */
	protected int makeQueuedJob(int time) {
		return makeJob(conn, null, QUEUED, null, ofEpochMilli(0), null, null,
				ofSeconds(time), now());
	}

	protected void makeAllocBySizeRequest(int job, int size) {
		try (var u = conn.update(TEST_INSERT_REQ_SIZE)) {
			conn.transaction(() -> u.call(job, size));
		}
	}

	protected void makeAllocByDimensionsRequest(int job, int width, int height,
			int allowedDead) {
		try (var u = conn.update(TEST_INSERT_REQ_DIMS)) {
			conn.transaction(() -> u.call(job, width, height, allowedDead));
		}
	}

	protected void makeAllocByBoardIdRequest(int job, int board) {
		try (var u = conn.update(TEST_INSERT_REQ_BOARD)) {
			conn.transaction(() -> u.call(job, board));
		}
	}

	protected JobState getJobState(int job) {
		try (var q = conn.query(GET_JOB)) {
			return conn.transaction(() -> q.call1(job).orElseThrow()
					.getEnum("job_state", JobState.class));
		}
	}

	protected int getJobRequestCount() {
		try (var q = conn.query(TEST_COUNT_REQUESTS)) {
			return conn.transaction(
					() -> q.call1(QUEUED).orElseThrow().getInt("cnt"));
		}
	}

	protected int getPendingPowerChanges() {
		try (var q = conn.query(TEST_COUNT_POWER_CHANGES)) {
			return conn
					.transaction(() -> q.call1().orElseThrow().getInt("cnt"));
		}
	}

	// Wrappers for temporarily putting the DB into a state with a job/alloc

	protected int makeJob() {
		return db.execute(c -> makeJob(c, null, QUEUED, null,
				ofEpochMilli(0), null, null, ofSeconds(0), now()));
	}

	protected void withJob(IntConsumer act) {
		int jobId = makeJob();
		try {
			act.accept(jobId);
		} finally {
			nukeJob(jobId);
		}
	}

	protected void nukeJob(int jobId) {
		db.executeVoid(c -> {
			try (var u = c.update("DELETE FROM jobs WHERE job_id = ?")) {
				u.call(jobId);
			}
		});
	}

	protected void withAllocation(int jobId, Runnable act) {
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

	protected void withStandardAllocatedJob(ObjIntConsumer<Permit> act) {
		// Composite op, for brevity
		withJob(jobId -> inContext(c -> withAllocation(jobId,
				() -> act.accept(c.setAuth(USER_NAME), jobId))));
	}

	private static final int DELAY_MS = 1000;

	/** Sleep for one second. */
	protected static void snooze1s() {
		try {
			Thread.sleep(DELAY_MS);
		} catch (InterruptedException e) {
			assumeTrue(false, "sleep() was interrupted");
		}
	}

	/** Capability provided by {@link #inContext(InC)} to what it guards. */
	public interface C {
		/**
		 * Install an authentication token.
		 *
		 * @param auth
		 *            The authentication token to install. {@code null} to
		 *            remove.
		 */
		void setAuth(Authentication auth);

		/**
		 * Install the named user as the current user and provide a permit that
		 * allows that user to touch resources.
		 *
		 * @param name
		 *            The user name.
		 * @return The newly-minted permit.
		 */
		default Permit setAuth(String name) {
			@SuppressWarnings("serial")
			var a = new Authentication() {
				@Override
				public String getName() {
					return name;
				}

				@Override
				public Collection<? extends GrantedAuthority> getAuthorities() {
					return List.of();
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
			};
			setAuth(a);
			return new Permit(SecurityContextHolder.getContext());
		}
	}

	/**
	 * An action wrapped by {@link #inContext(InC)}.
	 */
	public interface InC {
		/**
		 * The wrapped action.
		 *
		 * @param c
		 *            The capability to set the current user.
		 */
		void act(C c);
	}

	/**
	 * Run code with the capability to set the current user. Will clean up
	 * afterwards.
	 *
	 * @param inc
	 *            The code to run.
	 */
	public static void inContext(InC inc) {
		var context = SecurityContextHolder.getContext();
		try {
			inc.act(context::setAuthentication);
		} finally {
			context.setAuthentication(null);
		}
	}
}
