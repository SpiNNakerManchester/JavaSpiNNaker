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
package uk.ac.manchester.spinnaker.alloc.allocator;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.Constants.TRIAD_DEPTH;
import static uk.ac.manchester.spinnaker.alloc.db.Row.enumerate;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.isBusy;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.POWER;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.READY;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.OFF;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.ON;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.google.errorprone.annotations.RestrictedApi;

import uk.ac.manchester.spinnaker.alloc.ForTestingOnly;
import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AllocatorProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.HistoricalDataProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.KeepaliveProperties;
import uk.ac.manchester.spinnaker.alloc.bmp.BMPController;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Update;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.model.BoardAndBMP;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * The allocation engine. Allocations are performed by running suitable
 * (non-trivial) SQL queries on a periodic basis, putting jobs that cannot be
 * allocated back on the queue for a later attempt (and increasing their
 * priority when it does so). This class is also responsible for destroying jobs
 * that are not kept alive sufficiently frequently, and eventually migrating
 * records of dead jobs to long-term storage ("tombstoning").
 */
@Service
public class AllocatorTask extends DatabaseAwareBean
		implements PowerController {
	/**
	 * @see #setPower(Connection,int,PowerState)
	 */
	private static final EnumSet<Direction> NO_PERIMETER =
			EnumSet.noneOf(Direction.class);

	private static final Logger log = getLogger(AllocatorTask.class);

	private static final Rectangle ONE_BOARD = new Rectangle(1, 1, 1);

	/**
	 * Maximum number of jobs that we actually run the quota check for. Used
	 * because we are reusing a query, and we'll probably never have that many
	 * live jobs even on the big machine.
	 */
	private static final Integer NUMBER_OF_JOBS_TO_QUOTA_CHECK = 100000;

	@Autowired
	private Epochs epochs;

	@Autowired
	private ServiceMasterControl serviceControl;

	@Autowired
	private QuotaManager quotaManager;

	@Autowired
	private AllocatorProperties allocProps;

	@Autowired
	private KeepaliveProperties keepAliveProps;

	@Autowired
	private HistoricalDataProperties historyProps;

	@Autowired
	private ProxyRememberer rememberer;

	@Autowired
	private TaskScheduler scheduler;

	// Note can't be autowired as circular;
	// instead set by setter in postconstruct of BMPController
	private BMPController bmpController;

	public void setBMPController(BMPController bmpController) {
		this.bmpController = bmpController;
	}

	@PostConstruct
	@SuppressWarnings("FutureReturnValueIgnored")
	private void init() {
		scheduler.scheduleAtFixedRate(() -> allocate(),	allocProps.getPeriod());
		scheduler.scheduleAtFixedRate(() -> expireJobs(),
				keepAliveProps.getExpiryPeriod());
		scheduler.schedule(() -> tombstone(),
				new CronTrigger(historyProps.getSchedule()));
	}

	/**
	 * Perform update on a job now as a result of a change.
	 *
	 * @param jobId The job to update.
	 * @param sourceState The change source state.
	 * @param targetState The change target state.
	 */
	public void updateJob(int jobId, JobState sourceState,
			JobState targetState) {
		scheduler.schedule(() -> {
			updateJobNow(jobId, sourceState, targetState);
		}, Instant.now());
	}

	private void updateJobNow(int jobId, JobState sourceState,
			JobState targetState) {
		try {
			var updated = execute(
					conn -> update(jobId, sourceState, targetState, conn));
			if (updated) {
				log.debug("advancing job epoch");
				epochs.nextJobsEpoch();
			}
		} catch (DataAccessException e) {
			if (isBusy(e)) {
				log.info("database is busy; "
						+ "will try allocation processing later");
				return;
			}
			throw e;
		}
	}

	private boolean update(int jobId, JobState sourceState,
			JobState targetState, Connection c) {
		try (var getNTasks = c.query(COUNT_CHANGES_FOR_JOB);
				var setJobState = c.update(SET_STATE_PENDING)) {

			// Count pending changes for this state change
			var n = getNTasks.call1(row -> row.getInteger("n_changes"),
					jobId, sourceState, targetState).orElseThrow(
							() -> new RuntimeException(
									"Error counting job tasks"));

			log.debug("Job {} has {} changes remaining", jobId, n);

			// If there are no more pending changes, set the job state to
			// the target state
			if (n == 0) {
				log.debug("Job {} moving to state {}", jobId, targetState);
				setJobState.call(targetState, 0, jobId);
				return true;
			}
			return false;
		}
	}

	/**
	 * Helper class representing a rectangle of triads.
	 *
	 * @author Donal Fellows
	 */
	private static final class Rectangle {
		final int width;

		final int height;

		/** Depth of rectangle. 1 or 3 */
		final int depth;

		private Rectangle(int width, int height, int depth) {
			this.width = width;
			this.height = height;
			this.depth = depth;
		}

		private Rectangle(Row row) {
			this(row.getInt("max_width"), row.getInt("max_height"),
					TRIAD_DEPTH);
		}

		@Override
		public String toString() {
			return format("%dx%dx%d", width, height, depth);
		}

		public int getArea() {
			return width * height * depth;
		}
	}

	/**
	 * Allocate all current requests for resources.
	 */
	public synchronized void allocate() {
		if (serviceControl.isPaused()) {
			return;
		}

		try {
			var bmps = execute(this::allocate);
			if (bmps.size() > 0) {
				log.debug("advancing job epoch");
				epochs.nextJobsEpoch();

				// Poke the BMP controller to start looking!
				log.debug("Triggering BMPs {}", bmps);
				bmpController.triggerSearch(bmps);
			}
		} catch (DataAccessException e) {
			if (isBusy(e)) {
				log.info("database is busy; "
						+ "will try allocation processing later");
				return;
			}
			throw e;
		}
	}

	private class Perimeter {
		int boardId;

		Direction direction;

		Perimeter(Row row) {
			boardId = row.getInt("board_id");
			direction = row.getEnum("direction", Direction.class);
		}
	}

	/** Encapsulates the queries and updates used in power control. */
	private class PowerSQL extends AbstractSQL {
		/** Get basic information about a specific job. */
		private final Query getJobState;

		/** Get what boards are allocated to a job (that is queued or ready). */
		private final Query getJobBoards;

		/**
		 * Get the links on the perimeter of the allocation to a job. The
		 * perimeter is defined as being the links between a board that is part
		 * of the allocation and a board that is not; it's <em>not</em> a
		 * geometric definition, but rather a relational algebraic one.
		 */
		private final Query getPerimeter;

		/** Create a request to change the power status of a board. */
		private final Update issuePowerChange;

		/** Set the state and number of pending changes for a job. */
		private final Update setStatePending;

		/** Set the state to destroyed. */
		private final Update setStateDestroyed;

		PowerSQL(Connection conn) {
			super(conn);
			getJobState = conn.query(GET_JOB);
			getJobBoards = conn.query(GET_JOB_BOARDS);
			getPerimeter = conn.query(getPerimeterLinks);
			issuePowerChange = conn.update(issueChangeForJob);
			setStatePending = conn.update(SET_STATE_PENDING);
			setStateDestroyed = conn.update(SET_STATE_DESTROYED);
		}

		@Override
		public void close() {
			getJobState.close();
			getJobBoards.close();
			getPerimeter.close();
			issuePowerChange.close();
			setStatePending.close();
			setStateDestroyed.close();
		}
	}

	/** Encapsulates the queries and updates used in allocation. */
	@UsedInJavadocOnly(SQLQueries.class)
	private final class AllocSQL extends PowerSQL {
		/** Increases the importance of a job. */
		private final Update bumpImportance;

		/** Get the list of allocation tasks for jobs in a given state. */
		private final Query getTasks;

		/** Delete an allocation task. */
		private final Update delete;

		/** Find a single free board. */
		private final Query findFreeBoard;

		/**
		 * Find a rectangle of triads of boards that may be allocated.
		 *
		 * @see SQLQueries#FIND_FREE_BOARD
		 */
		private final Query getRectangles;

		/**
		 * Find a rectangle of triads of boards that may be allocated rooted at
		 * a particular board.
		 *
		 * @see SQLQueries#FIND_FREE_BOARD_AT
		 */
		private final Query getRectangleAt;

		/**
		 * Count the number of <em>connected</em> boards (i.e., have at least
		 * one path over enabled links to the root board of the allocation)
		 * within a rectangle of triads. The triads are taken as being full
		 * depth.
		 */
		private final Query countConnectedBoards;

		/**
		 * Find an allocatable board with a specific board ID. (This will have
		 * been previously converted from some other form of board coordinates.)
		 */
		private final Query findSpecificBoard;

		/**
		 * Get the set of boards at some coordinates within a triad rectangle
		 * that are connected (i.e., have at least one path over enableable
		 * links) to the root board.
		 */
		private final Query getConnectedBoardIDs;

		/** Tell a board that it is allocated. */
		private final Update allocBoard;

		/** Tell a job that it is allocated. Doesn't set the state. */
		private final Update allocJob;

		AllocSQL(Connection conn) {
			super(conn);
			bumpImportance = conn.update(BUMP_IMPORTANCE);
			getTasks = conn.query(getAllocationTasks);
			delete = conn.update(DELETE_TASK);
			findFreeBoard = conn.query(FIND_FREE_BOARD);
			getRectangles = conn.query(findRectangle);
			getRectangleAt = conn.query(findRectangleAt);
			countConnectedBoards = conn.query(countConnected);
			findSpecificBoard = conn.query(findLocation);
			getConnectedBoardIDs = conn.query(getConnectedBoards);
			allocBoard = conn.update(ALLOCATE_BOARDS_BOARD);
			allocJob = conn.update(ALLOCATE_BOARDS_JOB);
		}

		@Override
		public void close() {
			super.close();
			bumpImportance.close();
			getTasks.close();
			delete.close();
			findFreeBoard.close();
			getRectangles.close();
			getRectangleAt.close();
			countConnectedBoards.close();
			findSpecificBoard.close();
			getConnectedBoardIDs.close();
			allocBoard.close();
			allocJob.close();
		}
	}

	/** Encapsulates the queries and updates used in deletion. */
	private final class DestroySQL extends PowerSQL {
		/** Get basic information about a specific job. */
		private final Query getJob;

		/** Mark a job as dead. */
		private final Update markAsDestroyed;

		/** Delete a request to allocate resources for a job. */
		private final Update killAlloc;

		DestroySQL(Connection conn) {
			super(conn);
			getJob = conn.query(GET_JOB);
			markAsDestroyed = conn.update(DESTROY_JOB);
			killAlloc = conn.update(KILL_JOB_ALLOC_TASK);
		}

		@Override
		public void close() {
			super.close();
			getJob.close();
			markAsDestroyed.close();
			killAlloc.close();
		}
	}

	private class AllocTask {
		final int id;

		final int importance;

		final int jobId;

		final int machineId;

		final Rectangle max;

		final int maxDeadBoards;

		final Integer numBoards;

		final Integer width;

		final Integer height;

		final Integer root;

		AllocTask(Row row) {
			id = row.getInt("req_id");
			importance = row.getInt("importance");
			jobId = row.getInt("job_id");
			machineId = row.getInt("machine_id");
			max = new Rectangle(row);
			maxDeadBoards = row.getInt("max_dead_boards");
			numBoards = row.getInteger("num_boards");
			width = row.getInteger("width");
			height = row.getInteger("height");
			root = row.getInteger("board_id");
		}

		Collection<Integer> allocate(AllocSQL sql) {
			if (nonNull(numBoards) && numBoards > 0) {
				// Single-board case gets its own allocator that's better at
				// that
				if (numBoards == 1) {
					log.debug("Allocate one board");
					return allocateOneBoard(sql, jobId, machineId);
				}
				var estimate = new DimensionEstimate(numBoards, max);
				return allocateDimensions(sql, jobId, machineId, estimate,
						maxDeadBoards);
			}

			if (nonNull(width) && nonNull(height) && nonNull(root)) {
				return allocateTriadsAt(sql, jobId, machineId, root, width,
						height,	maxDeadBoards);
			}

			if (nonNull(width) && nonNull(height) && width > 0 && height > 0) {
				// Special case; user is really just asking for one board
				if (height == 1 && width == 1 && nonNull(maxDeadBoards)
						&& maxDeadBoards == 2) {
					return allocateOneBoard(sql, jobId, machineId);
				}
				var estimate = new DimensionEstimate(width, height, max);
				log.debug(
						"resolved request for {}x{} boards to {}x{} triads "
								+ "with tolerance {}",
						width, height, estimate.width, estimate.height,
						estimate.tolerance);
				return allocateDimensions(sql, jobId, machineId, estimate,
						maxDeadBoards);
			}

			if (nonNull(root)) {
				// Ignores maxDeadBoards; is a single-board allocate
				return allocateBoard(sql, jobId, machineId, root);
			}

			log.warn("job {} could not be allocated; "
					+ "bad request will be cleared from queue", jobId);
			return List.of();
		}
	}

	/**
	 * Allocate all current requests for resources.
	 *
	 * @param conn
	 *            The DB connection
	 * @return Whether any changes have been done
	 */
	private Collection<Integer> allocate(Connection conn) {
		try (var sql = new AllocSQL(conn)) {
			int maxImportance = -1;
			log.trace("Allocate running");
			var bmps = new HashSet<Integer>();
			for (AllocTask task : sql.getTasks.call(AllocTask::new, QUEUED)) {
				if (task.importance > maxImportance) {
					maxImportance = task.importance;
				} else if (task.importance < maxImportance
						- allocProps.getImportanceSpan()) {
					// Too much of a span
					continue;
				}
				var handled = task.allocate(sql);
				bmps.addAll(handled);
				log.debug("allocate for {} (job {}): {}", task.id,
						task.jobId, handled);
			}
			/*
			 * Those tasks which weren't allocated get their importance bumped
			 * so they get considered with higher priority when the allocator
			 * runs next time.
			 */
			sql.bumpImportance.call();
			return bmps;
		}
	}

	/**
	 * Destroy jobs that have missed their keepalive.
	 */
	public void expireJobs() {
		if (serviceControl.isPaused()) {
			return;
		}

		try {
			var bmps = execute(this::expireJobs);
			if (bmps.size() > 0) {
				epochs.nextJobsEpoch();
				epochs.nextMachineEpoch();

				bmpController.triggerSearch(bmps);
			}
		} catch (DataAccessException e) {
			if (isBusy(e)) {
				log.info("database is busy; "
						+ "will try job expiry processing later");
				return;
			}
			throw e;
		}
	}

	/**
	 * Destroy jobs that have missed their keepalive.
	 *
	 * @param conn
	 *            How to talk to the DB
	 * @return Whether any jobs have been expired.
	 */
	private Collection<Integer> expireJobs(Connection conn) {
		var bmps = new HashSet<Integer>();
		try (var find = conn.query(FIND_EXPIRED_JOBS)) {
			var toKill = find.call(integer("job_id"));
			for (var id : toKill) {
				bmps.addAll(destroyJob(conn, id, "keepalive expired"));
			}
		}
		try (var find = conn.query(GET_LIVE_JOB_IDS)) {
			var toKill = find.call(integer("job_id"),
					NUMBER_OF_JOBS_TO_QUOTA_CHECK, 0);
			for (var id : toKill) {
				if (quotaManager.shouldKillJob(id)) {
					bmps.addAll(destroyJob(conn, id, "quota exceeded"));
				}
			}
		}
		return bmps;
	}

	/**
	 * Migrates long dead jobs to the historical data DB.
	 */
	public void tombstone() {
		if (serviceControl.isPaused()) {
			return;
		}

		try (var conn = getConnection();
				var histConn = getHistoricalConnection()) {
			var c = tombstone(conn, histConn);
			log.info("tombstoning completed: "
					+ "moved {} job records and {} allocation records",
					c.numJobs(), c.numAllocs());
		} catch (DataAccessException e) {
			if (isBusy(e)) {
				log.info("database is busy; "
						+ "will try job tombstone processing at future date");
				return;
			}
			throw e;
		}
	}

	/**
	 * Describes what the first stage of the tombstoner has copied.
	 */
	static final class Copied {
		private final List<HistoricalJob> jobs;

		private final List<HistoricalAlloc> allocs;

		private Copied(List<HistoricalJob> jobs, List<HistoricalAlloc> allocs) {
			this.jobs = jobs;
			this.allocs = allocs;
		}

		private Stream<HistoricalAlloc> allocs() {
			return allocs.stream().filter(Objects::nonNull);
		}

		private Stream<HistoricalJob> jobs() {
			return jobs.stream().filter(Objects::nonNull);
		}

		/**
		 * @return The number of job records to copy over to the historical
		 *         database.
		 */
		int numJobs() {
			return jobs.size();
		}

		/**
		 * @return The number of board allocation records to copy over to the
		 *         historical database.
		 */
		int numAllocs() {
			return allocs.size();
		}
	}

	private class HistoricalAlloc {
		int allocId;

		int jobId;

		int boardId;

		Instant allocTimestamp;

		HistoricalAlloc(Row row) {
			allocId = row.getInt("alloc_id");
			jobId = row.getInt("job_id");
			boardId = row.getInt("board_id");
			allocTimestamp = row.getInstant("alloc_timestamp");
		}

		Object[] args() {
			return new Object[] {
				allocId, jobId, boardId, allocTimestamp
			};
		}
	}

	private class HistoricalJob {
		int jobId;

		int machineId;

		String owner;

		Instant createTimestamp;

		int width;

		int height;

		int depth;

		int allocatedRoot;

		Instant keepaliveInterval;

		String keepaliveHost;

		String deathReason;

		Instant deathTimestamp;

		byte[] originalRequest;

		Instant allocationTimestamp;

		int allocationSize;

		String machineName;

		String userName;

		int groupId;

		String groupName;

		HistoricalJob(Row row) {
			jobId = row.getInt("job_id");
			machineId = row.getInt("machine_id");
			owner = row.getString("owner");
			createTimestamp = row.getInstant("create_timestamp");
			width = row.getInt("width");
			height = row.getInt("height");
			depth = row.getInt("depth");
			allocatedRoot = row.getInt("allocated_root");
			keepaliveInterval = row.getInstant("keepalive_interval");
			keepaliveHost = row.getString("keepalive_host");
			deathReason = row.getString("death_reason");
			deathTimestamp = row.getInstant("death_timestamp");
			originalRequest = row.getBytes("original_request");
			allocationTimestamp = row.getInstant("allocation_timestamp");
			allocationSize = row.getInt("allocation_size");
			machineName = row.getString("machine_name");
			userName = row.getString("user_name");
			groupId = row.getInt("group_id");
			groupName = row.getString("group_name");
		}

		Object[] args() {
			return new Object[] {
				jobId, machineId, owner, createTimestamp,
				width, height, depth, allocatedRoot, keepaliveInterval,
				keepaliveHost, deathReason, deathTimestamp, originalRequest,
				allocationTimestamp, allocationSize, machineName, userName,
				groupId, groupName
			};
		}
	}

	/**
	 * Implementation of {@link #tombstone()}. This is done as two transactions
	 * to help manage the amount of locking (especially multi-DB locking);
	 * nothing else ought to be updating any of these jobs at the time this task
	 * usually runs, but we'll still try to keep things minimally locked.
	 *
	 * @param conn
	 *            The DB connection
	 * @return Description of the tombstoned IDs
	 */
	private Copied tombstone(Connection conn, Connection histConn) {
		// No tombstoning without the target DB!
		if (!isHistoricalDBAvailable()) {
			return new Copied(List.of(), List.of());
		}

		try (var readJobs = conn.query(READ_HISTORICAL_JOBS);
				var readAllocs = conn.query(READ_HISTORICAL_ALLOCS);
				var deleteJobs = conn.update(DELETE_JOB_RECORD);
				var deleteAllocs = conn.update(DELETE_ALLOC_RECORD);
				var writeJobs = histConn.update(WRITE_HISTORICAL_JOBS);
				var writeAllocs = histConn.update(WRITE_HISTORICAL_ALLOCS)) {
			var grace = historyProps.getGracePeriod();
			var copied = conn.transaction(
					() -> new Copied(readJobs.call(HistoricalJob::new, grace),
							readAllocs.call(HistoricalAlloc::new, grace)));
			histConn.transaction(() -> {
				copied.allocs().forEach((a) -> writeAllocs.call(a.args()));
				copied.jobs().forEach((j) -> writeJobs.call(j.args()));
			});
			conn.transaction(() -> {
				copied.allocs().forEach((a) -> deleteAllocs.call(a.allocId));
				copied.jobs().forEach((j) -> deleteJobs.call(j.jobId));
			});
			return copied;
		}
	}

	@Override
	public void destroyJob(int id, String reason) {
		var bmps = execute(conn -> destroyJob(conn, id, reason));
		if (bmps.size() > 0) {
			epochs.nextJobsEpoch();
			epochs.nextMachineEpoch();

			bmpController.triggerSearch(bmps);
		}
	}

	/**
	 * Destroy a job.
	 *
	 * @param conn
	 *            How to talk to the DB
	 * @param id
	 *            The ID of the job
	 * @param reason
	 *            Why is the job being destroyed.
	 * @return Whether the job was destroyed.
	 */
	private Collection<Integer> destroyJob(Connection conn, int id,
			String reason) {
		JobLifecycle.log.info("destroying job {} \"{}\"", id, reason);
		try (var sql = new DestroySQL(conn)) {
			if (sql.getJob.call1(enumerate("job_state", JobState.class), id)
					.orElse(DESTROYED) == DESTROYED) {
				/*
				 * Don't do anything if the job doesn't exist or is already
				 * destroyed
				 */
				return List.of();
			}
			// Inserts into pending_changes; these run after job is dead
			var bmps = setPower(sql, id, OFF, DESTROYED);
			sql.killAlloc.call(id);
			sql.markAsDestroyed.call(reason, id);
			return bmps;
		} finally {
			quotaManager.finishJob(id);
			rememberer.killProxies(id);
		}
	}

	/**
	 * Computes the estimate of what sort of allocation will be required.
	 * Converts a number of boards into a close-to-square size to search for.
	 * <p>
	 * With the big machine's level of resources, that's good enough. For now.
	 *
	 * @author Donal Fellows
	 */
	private static final class DimensionEstimate {
		/** The estimated width, in triads. */
		final int width;

		/** The estimated height, in triads. */
		final int height;

		/**
		 * The number of boards in the rectangle of triads that we can tolerate
		 * being down due to overallocation (due to the use of rectangles and
		 * triads).
		 */
		final int tolerance;

		/**
		 * Create an estimate of what to allocate. The old spalloc would take
		 * hints at this point on the aspect ratio, but we don't bother; we
		 * strongly prefer allocations "nearly square", going for making them
		 * slightly taller than wide if necessary.
		 *
		 * @param numBoards
		 *            The number of boards wanted.
		 * @param max
		 *            The size of the machine.
		 */
		DimensionEstimate(int numBoards, Rectangle max) {
			if (numBoards < 1) {
				throw new IllegalArgumentException(
						"number of boards must be greater than zero");
			}
			int numTriads = ceildiv(numBoards, TRIAD_DEPTH);
			width = min((int) ceil(sqrt(numTriads)), max.width);
			height = min(ceildiv(numTriads, width), max.height);
			tolerance = (width * height * TRIAD_DEPTH) - numBoards;
			if (width < 1 || height < 1) {
				throw new IllegalArgumentException(
						"computed dimensions must be greater than zero");
			}
			if (tolerance < 0) {
				throw new IllegalArgumentException(
						"that job cannot possibly fit on this machine");
			}
		}

		/**
		 * Create an estimate of what to allocate. This does not need to be
		 * "near square".
		 *
		 * @param w
		 *            The width of the allocation requested, in triads.
		 * @param h
		 *            The height of the allocation requested, in triads.
		 * @param max
		 *            The size of the machine.
		 */
		DimensionEstimate(int w, int h, Rectangle max) {
			if (w < 1 || h < 1) {
				throw new IllegalArgumentException(
						"dimensions must be greater than zero");
			}
			int numBoards = w * h * TRIAD_DEPTH;
			width = max(1, min(w, max.width));
			height = max(1, min(h, max.height));
			tolerance = (width * height * TRIAD_DEPTH) - numBoards;
			if (tolerance < 0) {
				throw new IllegalArgumentException(
						"that job cannot possibly fit on this machine");
			}
		}

		/** @return The estimated dimensions as a rectangle. */
		Rectangle getRect() {
			return new Rectangle(width, height, TRIAD_DEPTH);
		}
	}

	private Collection<Integer> allocateOneBoard(AllocSQL sql, int jobId,
			int machineId) {
		// This is simplified; no subsidiary searching needed
		return sql.findFreeBoard
				.call1(row -> setAllocation(sql, jobId,
						ONE_BOARD, machineId, coords(row)), machineId)
				.orElse(List.of());
	}

	private static TriadCoords coords(Row row) {
		int x = row.getInt("x");
		int y = row.getInt("y");
		int z = row.getInt("z");
		return new TriadCoords(x, y, z);
	}

	private Collection<Integer> allocateDimensions(AllocSQL sql, int jobId,
			int machineId, DimensionEstimate estimate, int userMaxDead) {
		int tolerance = userMaxDead + estimate.tolerance;
		int minArea =
				estimate.width * estimate.height * TRIAD_DEPTH - tolerance;
		for (var root : sql.getRectangles
				.call(AllocatorTask::coords, estimate.width, estimate.height,
						machineId, tolerance)) {
			if (minArea > 1) {
				/*
				 * Check that a minimum number of boards are reachable from the
				 * proposed root board. If the root board is isolated, we don't
				 * care if the rest of the allocation works because the rest of
				 * the toolchain won't cope.
				 */
				int size = connectedSize(sql, machineId, root, estimate);
				if (size < minArea) {
					continue;
				}
			}
			return setAllocation(sql, jobId, estimate.getRect(), machineId,
					root);
		}
		log.debug("Could not allocate min area {}", minArea);
		return List.of();
	}

	/**
	 * Find the number of boards that are reachable from the proposed root
	 * board.
	 *
	 * @param sql
	 *            How to talk to the DB
	 * @param machineId
	 *            The machine on which the allocation is happening
	 * @param root
	 *            Root logical coordinates
	 * @param width
	 *            The width of the planned allocation, in triads
	 * @param height
	 *            The width of the planned allocation, in triads
	 * @return How many boards in the allocation are reachable.
	 */
	private int connectedSize(AllocSQL sql, int machineId, TriadCoords root,
			int width, int height) {
		return sql.countConnectedBoards
				.call1(integer("connected_size"), machineId, root.x, root.y,
						width, height).orElse(-1);
	}

	/**
	 * Find the number of boards that are reachable from the proposed root
	 * board.
	 *
	 * @param sql
	 *            How to talk to the DB
	 * @param machineId
	 *            The machine on which the allocation is happening
	 * @param root
	 *            Root logical coordinates
	 * @param rect
	 *            The requested allocation dimensions
	 * @return How many boards in the allocation are reachable.
	 */
	private int connectedSize(AllocSQL sql, int machineId, TriadCoords root,
			Rectangle rect) {
		return connectedSize(sql, machineId, root, rect.width, rect.height);
	}

	/**
	 * Find the number of boards that are reachable from the proposed root
	 * board.
	 *
	 * @param sql
	 *            How to talk to the DB
	 * @param machineId
	 *            The machine on which the allocation is happening
	 * @param root
	 *            Root logical coordinates
	 * @param estimate
	 *            The planned allocation dimensions
	 * @return How many boards in the allocation are reachable.
	 */
	private int connectedSize(AllocSQL sql, int machineId, TriadCoords root,
			DimensionEstimate estimate) {
		return connectedSize(sql, machineId, root, estimate.width,
				estimate.height);
	}

	private Collection<Integer> allocateBoard(AllocSQL sql, int jobId,
			int machineId, int boardId) {
		return sql.findSpecificBoard
				.call1(row -> setAllocation(sql, jobId, ONE_BOARD, machineId,
						coords(row)), machineId, boardId)
				.orElse(List.of());
	}

	private Collection<Integer> allocateTriadsAt(AllocSQL sql, int jobId,
			int machineId, int rootId, int width, int height,
			int maxDeadBoards) {
		var rect = new Rectangle(width, height, TRIAD_DEPTH);
		return sql.getRectangleAt
				.call1(AllocatorTask::coords, rootId, width, height, machineId,
						maxDeadBoards)
				.filter(root -> connectedSize(sql, machineId, root,
						rect) >= rect.getArea() - maxDeadBoards)
				.map(root -> setAllocation(sql, jobId, rect, machineId, root))
				.orElse(List.of());
	}

	/**
	 * Does the actual allocation.
	 * <p>
	 * At this point, we've checked that there's enough boards <em>and</em> we
	 * are running in a transaction. We take particular care that we only
	 * actually allocate boards that are reachable from the root board; this is
	 * assumed by the SpiNNaker tools, so we'd better conform to that
	 * expectation. Fortunately, the bigger the allocation, the more likely that
	 * is true (and it is trivially true for single-board allocations.)
	 * <p>
	 * If you want a multi-board allocation, you'd better be allocating a full
	 * triad's-worth of depth or you'll get nothing.
	 *
	 * @param sql
	 *            How to talk to the DB
	 * @param jobId
	 *            What job are we allocating for
	 * @param rect
	 *            Proposed rectangle size
	 * @param machineId
	 *            What machine are we allocating on
	 * @param root
	 *            Proposed root coordinates
	 * @return The BMPs that have been used to make the allocation.
	 */
	private Collection<Integer> setAllocation(AllocSQL sql, int jobId,
			Rectangle rect,	int machineId, TriadCoords root) {
		log.debug("performing allocation for {}: {}x{}x{} at {}:{}:{}", jobId,
				rect.width, rect.height, rect.depth, root.x, root.y, root.z);
		var boardsToAllocate = sql.getConnectedBoardIDs
				.call(integer("board_id"), machineId, root.x, root.y, root.z,
						rect.width, rect.height, rect.depth);
		if (boardsToAllocate.isEmpty()) {
			log.debug("No boards to allocate");
			return List.of();
		}
		for (var boardId : boardsToAllocate) {
			sql.allocBoard.call(jobId, boardId);
		}

		var board = boardsToAllocate.get(0);
		sql.allocJob.call(rect.width, rect.height, rect.depth,
				board, boardsToAllocate.size(), board, jobId);
		log.info("allocated {} boards to {}; issuing power up commands",
				boardsToAllocate.size(), jobId);
		// Any proxies that existed are now defunct; user must make anew
		rememberer.killProxies(jobId);
		return setPower(sql, jobId, ON, READY);
	}

	/**
	 * Reset a job after a failure on a BMP.
	 *
	 * @param jobId The identifier of the job to reset.
	 */
	@SuppressWarnings("FutureReturnValueIgnored")
	public void resetPowerOnFailure(int jobId) {
		scheduler.schedule(() -> setPower(jobId, OFF, QUEUED), Instant.now());
	}

	@Override
	public boolean setPower(int jobId, PowerState power, JobState targetState) {
		var bmps = execute(conn -> {
			try (var sql = new PowerSQL(conn)) {
				return setPower(sql, jobId, power, targetState);
			}
		});
		if (bmps.size() > 0) {
			rememberer.killProxies(jobId);
			epochs.nextMachineEpoch();
			epochs.nextJobsEpoch();

			// Poke the BMP controller to start work!
			log.debug("Triggering BMPs {}", bmps);
			bmpController.triggerSearch(bmps);
			return true;
		}
		return false;
	}

	/**
	 * Issue a request to change the power for the boards of a job.
	 *
	 * @param sql
	 *            How to talk to the DB
	 * @param jobId
	 *            The job in question
	 * @param power
	 *            The power state to switch to
	 * @param targetState
	 *            The state to put the job in afterwards
	 * @return The ids of the BMPs that have been changed
	 */
	private Collection<Integer> setPower(PowerSQL sql, int jobId,
			PowerState power, JobState targetState) {
		var sourceState = sql.getJobState.call1(
				enumerate("job_state", JobState.class), jobId).orElseThrow();
		var boards = sql.getJobBoards.call(BoardAndBMP::new, jobId);
		if (boards.isEmpty()) {
			log.debug("No boards for job {}", jobId);
			if (targetState == DESTROYED) {
				log.debug("no boards for {} in destroy", jobId);
			}
			return List.of();
		}
		log.debug("{} boards for job {}", boards.size(), jobId);

		// Number of changes pending, one per board
		int numPending = 0;

		var bmps = new HashSet<Integer>();
		if (power == ON) {
			/*
			 * This is a bit of a trickier case, as we need to say which links
			 * are to be switched on or, more particularly, which are to be
			 * switched off because they are links to boards that are not
			 * allocated to the job. Off-board links are shut off by default.
			 */
			var perimeterLinks = Row.stream(
					sql.getPerimeter.call(Perimeter::new, jobId))
					.toCollectingMap(Direction.class, (p) -> p.boardId,
							(p) -> p.direction);

			for (var board : boards) {
				var toChange = perimeterLinks.getOrDefault(board.boardId,
						NO_PERIMETER);
				numPending += sql.issuePowerChange.call(jobId,
						board.boardId,
						sourceState, targetState, true,
						!toChange.contains(Direction.N),
						!toChange.contains(Direction.E),
						!toChange.contains(Direction.SE),
						!toChange.contains(Direction.S),
						!toChange.contains(Direction.W),
						!toChange.contains(Direction.NW));
				bmps.add(board.bmpId);
			}
		} else {
			// Powering off; all links switch to off so no perimeter check
			for (var board : boards) {
				numPending += sql.issuePowerChange.call(jobId,
						board.boardId,	sourceState, targetState,
						false, false, false, false,	false, false, false);
				bmps.add(board.bmpId);
			}
		}

		if (targetState == DESTROYED) {
			log.debug("num changes for {} in destroy: {}", jobId, numPending);
			sql.setStateDestroyed.call(numPending, jobId);
		} else {
			log.debug("Num changes for target {}: {}", targetState, numPending);
			sql.setStatePending.call(
				numPending > 0 ? POWER : targetState,
				numPending, jobId);
		}

		return bmps;
	}

	/**
	 * Operations for testing only.
	 *
	 * @hidden
	 */
	@ForTestingOnly
	interface TestAPI {
		/**
		 * Allocate all current requests for resources.
		 *
		 * @return The BMPs updated by the allocation.
		 */
		Collection<Integer> allocate();

		/**
		 * Destroy a job.
		 *
		 * @param id
		 *            The ID of the job
		 * @param reason
		 *            Why is the job being destroyed.
		 * @return The BMPs updated by the destruction.
		 */
		Collection<Integer> destroyJob(int id, String reason);

		/**
		 * Destroy jobs that have missed their keepalive.
		 *
		 * @return The BMPs updated by the expiry.
		 */
		Collection<Integer> expireJobs();
	}

	/** Operations for testing historical database only. */
	@ForTestingOnly
	interface HistTestAPI {


		/**
		 * Implementation of {@link AllocatorTask#tombstone()}.
		 *
		 * @return Description of the tombstoned IDs
		 */
		Copied tombstone();
	}

	/**
	 * @param conn
	 *            The DB connection
	 * @return The test interface.
	 * @deprecated This interface is just for testing.
	 * @hidden
	 */
	@ForTestingOnly
	@RestrictedApi(explanation = "just for testing", link = "index.html",
			allowedOnPath = ".*/src/test/java/.*")
	@Deprecated
	TestAPI getTestAPI(Connection conn) {
		ForTestingOnly.Utils.checkForTestClassOnStack();
		return new TestAPI() {
			@Override
			public Collection<Integer> allocate() {
				return AllocatorTask.this.allocate(conn);
			}

			@Override
			public Collection<Integer> destroyJob(int id, String reason) {
				return AllocatorTask.this.destroyJob(conn, id, reason);
			}

			@Override
			public Collection<Integer> expireJobs() {
				return AllocatorTask.this.expireJobs(conn);
			}
		};
	}

	/**
	 * @param conn
	 *            The DB connection
	 * @param histConn
	 *            The historical DB connection
	 * @return The test interface.
	 * @deprecated This interface is just for testing.
	 */
	@ForTestingOnly
	@RestrictedApi(explanation = "just for testing", link = "index.html",
			allowedOnPath = ".*/src/test/java/.*")
	@Deprecated
	HistTestAPI getHistTestAPI(Connection conn, Connection histConn) {
		ForTestingOnly.Utils.checkForTestClassOnStack();
		return new HistTestAPI() {
			@Override
			public Copied tombstone() {
				return AllocatorTask.this.tombstone(conn, histConn);
			}
		};
	}
}
