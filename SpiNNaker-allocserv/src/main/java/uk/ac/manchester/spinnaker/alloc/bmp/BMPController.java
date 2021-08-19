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
package uk.ac.manchester.spinnaker.alloc.bmp;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static org.slf4j.MDC.putCloseable;
import static org.sqlite.SQLiteErrorCode.SQLITE_BUSY;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.UNKNOWN;
import static uk.ac.manchester.spinnaker.messages.model.FPGALinkRegisters.STOP;
import static uk.ac.manchester.spinnaker.messages.model.FPGAMainRegisters.FLAG;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_OFF;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_ON;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.MDC.MDCCloseable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.sqlite.SQLiteException;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Transacted;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.transceiver.BMPTransceiverInterface;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;

/**
 * Manages the BMPs of machines controlled by Spalloc.
 *
 * @author Donal Fellows
 */
@Service("bmpController")
@ManagedResource("Spalloc:type=BMPController,name=bmpController")
public class BMPController extends SQLQueries {
	private static final Logger log = getLogger(BMPController.class);

	private static final int FPGA_FLAG_ID_MASK = 0x3;

	private static final int NUM_FPGA_RETRIES = 3;

	private static final int BMP_VERSION_MIN = 2;

	private static final int SECONDS_BETWEEN_TRIES = 15;

	private static final int N_REQUEST_TRIES = 2;

	private static final int MS_PER_S = 1000;

	private static final long INTER_TAKE_DELAY = 10000;

	/**
	 * We <em>always</em> talk to the root BMP of a machine, and never directly
	 * to any others. The BMPs use I<sup>2</sup>C to communicate with each other
	 * on our behalf. Note that this also means that board numbers are
	 * necessarily unique within a machine.
	 * <p>
	 * This same strategy was also used by the original {@code spalloc}.
	 */
	private static final BMPCoords ROOT_BMP = new BMPCoords(0, 0);

	private boolean stop;

	//private Map<Machine, Thread> workers = new HashMap<>();

	private Map<Machine, WorkerState> state = new HashMap<>();

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private SpallocAPI spallocCore;

	@Autowired
	private ServiceMasterControl serviceControl;

	@Autowired
	private TransceiverFactoryAPI<?> txrxFactory;

	@Autowired
	private Epochs epochs;

	private final ThreadGroup group = new ThreadGroup("BMP workers");

	/** We have our own pool. */
	private ExecutorService executor = newCachedThreadPool(this::makeThread);

	/**
	 * A {@link ThreadFactory}.
	 *
	 * @param target
	 *            What the thread will be doing.
	 * @return The thread.
	 */
	private Thread makeThread(Runnable target) {
		Thread t = new Thread(group, target);
		t.setUncaughtExceptionHandler(this::handleException);
		return t;
	}

	/**
	 * An {@link UncaughtExceptionHandler}.
	 *
	 * @param thread
	 *            The thread with the problem.
	 * @param exception
	 *            The exception that describes the problem.
	 */
	private void handleException(Thread thread, Throwable exception) {
		log.error("uncaught exception in BMP worker {}", thread, exception);
	}

	// ----------------------------------------------------------------
	// CORE BMP ACCESS FUNCTIONS

	/**
	 * Check whether an FPGA has come up in a good state.
	 *
	 * @param machine
	 *            The machine hosting the board with the FPGA.
	 * @param txrx
	 *            The transceiver for talking to the machine.
	 * @param board
	 *            Which board is the FPGA on?
	 * @param fpga
	 *            Which FPGA (0, 1, or 2) is being tested?
	 * @return True if the FPGA is in a correct state, false otherwise.
	 * @throws ProcessException
	 *             If a BMP rejects a message.
	 * @throws IOException
	 *             If network I/O fails.
	 */
	private static boolean isGoodFPGA(Machine machine,
			BMPTransceiverInterface txrx, Integer board, FpgaIdentifiers fpga)
			throws ProcessException, IOException {
		int flag = txrx.readFPGARegister(fpga.ordinal(), FLAG, ROOT_BMP, board);
		// FPGA ID is bottom two bits of FLAG register
		int fpgaId = flag & FPGA_FLAG_ID_MASK;
		boolean ok = fpgaId == fpga.ordinal();
		if (!ok) {
			log.warn("{} on board {} of {} has incorrect FPGA ID flag {}", fpga,
					board, machine.getName(), fpgaId);
		}
		return ok;
	}

	/**
	 * Is a board new enough to be able to manage FPGAs?
	 *
	 * @param txrx
	 *            Transceiver for talking to a BMP in a machine.
	 * @param board
	 *            The board number.
	 * @return True if the board can manage FPGAs.
	 * @throws ProcessException
	 *             If a BMP rejects a message.
	 * @throws IOException
	 *             If network I/O fails.
	 */
	private static boolean canBoardManageFPGAs(BMPTransceiverInterface txrx,
			Integer board) throws ProcessException, IOException {
		VersionInfo vi = txrx.readBMPVersion(ROOT_BMP, board);
		return vi.versionNumber.majorVersion >= BMP_VERSION_MIN;
	}

	/**
	 * Turns a link off. (We never need to explicitly switch a link on; that's
	 * implicit in switching on its board.)
	 * <p>
	 * Technically, switching a link off just switches off <em>sending</em> on
	 * that link. We assume that the other end of the link also behaves.
	 *
	 * @param txrx
	 *            The transceiver.
	 * @param board
	 *            The board.
	 * @param link
	 *            The link direction.
	 * @throws ProcessException
	 *             If a BMP rejects a message.
	 * @throws IOException
	 *             If network I/O fails.
	 */
	private static void setLinkOff(BMPTransceiverInterface txrx, Integer board,
			Direction link) throws ProcessException, IOException {
		// skip FPGA link configuration if old BMP version
		if (!canBoardManageFPGAs(txrx, board)) {
			return;
		}
		txrx.writeFPGARegister(link.fpga, link.bank, STOP, 1, ROOT_BMP, board);
	}

	/**
	 * Switch on a collection of boards on a machine and check that they've come
	 * up correctly.
	 * <p>
	 * Note that this operation can take some time.
	 *
	 * @param machine
	 *            The machine containing the boards.
	 * @param txrx
	 *            The transceiver for talking to the machine.
	 * @param boards
	 *            Which boards to switch on.
	 * @throws ProcessException
	 *             If a BMP sends a failure message.
	 * @throws IOException
	 *             If network I/O fails or we reach the limit on retries.
	 * @throws InterruptedException
	 *             If we're interrupted.
	 */
	private static void powerOnAndCheck(Machine machine,
			BMPTransceiverInterface txrx, List<Integer> boards)
			throws ProcessException, InterruptedException, IOException {
		List<Integer> boardsToPower = boards;
		for (int attempt = 1; attempt <= NUM_FPGA_RETRIES; attempt++) {
			txrx.power(POWER_ON, ROOT_BMP, boardsToPower);

			/*
			 * Check whether all the FPGAs on each board have come up correctly.
			 * If not, we'll need to try booting that board again. The boards
			 * that have booted correctly need no further action.
			 */

			List<Integer> retryBoards = new ArrayList<>();
			for (Integer board : boardsToPower) {
				// Skip board if old BMP version
				if (!canBoardManageFPGAs(txrx, board)) {
					continue;
				}

				for (FpgaIdentifiers fpga : FpgaIdentifiers.values()) {
					if (!isGoodFPGA(machine, txrx, board, fpga)) {
						retryBoards.add(board);
						/*
						 * Stop the INNERMOST loop; we know this board needs
						 * retrying so there's no point in continuing to look at
						 * the FPGAs it has.
						 */
						break;
					}
				}
			}
			if (retryBoards.isEmpty()) {
				// Success!
				return;
			}
			boardsToPower = retryBoards;
		}
		throw new IOException(
				"Could not get correct FPGA ID for " + boardsToPower.size()
						+ " boards after " + NUM_FPGA_RETRIES + " tries");
	}

	/**
	 * Change the power state of a board.
	 *
	 * @param txrx
	 *            The transceiver to use
	 * @param request
	 *            The change to carry out
	 * @throws ProcessException
	 *             If the transceiver chokes
	 * @throws InterruptedException
	 *             If interrupted
	 * @throws IOException
	 *             If network I/O fails
	 */
	private static void changeBoardPowerState(BMPTransceiverInterface txrx,
			Request request)
			throws ProcessException, InterruptedException, IOException {
		// Send any power on commands
		if (!request.powerOnBoards.isEmpty()) {
			powerOnAndCheck(request.machine, txrx, request.powerOnBoards);
		}

		// Process link requests next
		for (Link linkReq : request.linkRequests) {
			// Set the link state, as required
			setLinkOff(txrx, linkReq.board, linkReq.link);
		}

		// Finally send any power off commands
		if (!request.powerOffBoards.isEmpty()) {
			txrx.power(POWER_OFF, ROOT_BMP, request.powerOffBoards);
		}
	}

	// ----------------------------------------------------------------
	// SERVICE IMPLEMENTATION

	@Scheduled(fixedDelay = INTER_TAKE_DELAY, initialDelay = INTER_TAKE_DELAY)
	void mainSchedule() throws SQLException, IOException, SpinnmanException {
		if (serviceControl.isPaused()) {
			return;
		}

		try {
			processRequests();
		} catch (SQLiteException e) {
			if (e.getResultCode().equals(SQLITE_BUSY)) {
				log.info("database is busy; will try power processing later");
				return;
			}
			throw e;
		} catch (InterruptedException e) {
			log.error("interrupted while spawning a worker", e);
		}
	}

	/**
	 * The core of {@link #mainSchedule()}.
	 *
	 * @deprecated Only {@code public} for testing purposes.
	 * @throws SQLException
	 *             If DB access fails
	 * @throws IOException
	 *             If talking to the network fails
	 * @throws SpinnmanException
	 *             If a BMP sends an error back
	 * @throws InterruptedException
	 *             If the wait for workers to spawn fails.
	 */
	@Deprecated
	public void processRequests() throws SQLException, IOException,
			SpinnmanException, InterruptedException {
		for (Request req : takeRequests()) {
			addRequestToBMPQueue(req);
		}
	}

	/**
	 * Gets an estimate of the number of requests pending. This may include
	 * active requests that are being processed.
	 *
	 * @return The number of requests in the database queue.
	 * @throws SQLException
	 *             If anything goes wrong with the DB access.
	 */
	@ManagedAttribute(
			description = "An estimate of the number of requests " + "pending.")
	public int getPendingRequestLoading() throws SQLException {
		try (Connection conn = db.getConnection();
				Query countChanges = query(conn, COUNT_PENDING_CHANGES)) {
			Optional<Row> row = countChanges.call1();
			if (row.isPresent()) {
				return row.get().getInt("c");
			}
		}
		return 0;
	}

	/**
	 * Gets an estimate of the number of requests actually being processed.
	 *
	 * @return The number of requests on the active queue.
	 */
	@ManagedAttribute(description = "An estimate of the number of requests "
			+ "actually being processed.")
	public synchronized int getActiveRequestLoading() {
		return state.values().stream().mapToInt(s -> s.requests.size()).sum();
	}

	/**
	 * Describes a request to modify the power status of a collection of boards.
	 * The boards must be on a single machine and must all be assigned to a
	 * single job.
	 * <p>
	 * This is the message that is sent from the main thread to the per-BMP
	 * worker threads.
	 *
	 * @author Donal Fellows
	 */
	private final class Request {
		private final Machine machine;

		private final List<Integer> powerOnBoards;

		private final List<Integer> powerOffBoards;

		private final List<Link> linkRequests;

		private final Integer jobId;

		private final JobState from;

		private final JobState to;

		private final List<Integer> changeIds;

		/**
		 * Create a request.
		 *
		 * @param machine
		 *            What machine are the boards on? <em>Must not</em> be
		 *            {@code null}.
		 * @param powerOnBoards
		 *            What boards (by physical ID) are to be powered on? May be
		 *            {@code null}; that's equivalent to the empty list.
		 * @param powerOffBoards
		 *            What boards (by physical ID) are to be powered off? May be
		 *            {@code null}; that's equivalent to the empty list.
		 * @param linkRequests
		 *            Any link power control requests. By default, links are on
		 *            if their board is on and they are connected; it is
		 *            <em>useful and relevant</em> to modify the power state of
		 *            links on the periphery of an allocation. May be
		 *            {@code null}; that's equivalent to the empty list.
		 * @param jobId
		 *            For what job is this?
		 * @param from
		 *            What state is the job moving from?
		 * @param to
		 *            What state is the job moving to?
		 * @param changeIds
		 *            The DB ids that describe the change, so we can update
		 *            those records.
		 */
		@SuppressWarnings("checkstyle:ParameterNumber")
		Request(Machine machine, List<Integer> powerOnBoards,
				List<Integer> powerOffBoards, List<Link> linkRequests,
				Integer jobId, JobState from, JobState to,
				List<Integer> changeIds) {
			this.machine = requireNonNull(machine);
			this.powerOnBoards = new ArrayList<>(
					isNull(powerOnBoards) ? emptyList() : powerOnBoards);
			this.powerOffBoards = new ArrayList<>(
					isNull(powerOffBoards) ? emptyList() : powerOffBoards);
			this.linkRequests = new ArrayList<>(
					isNull(linkRequests) ? emptyList() : linkRequests);
			this.jobId = jobId;
			this.from = from;
			this.to = to;
			this.changeIds = changeIds;
		}

		/**
		 * Declare this to be a successful request.
		 */
		private void done() {
			processAfterChange(this, false);
		}

		/**
		 * Declare this to be a failed request.
		 */
		private void failed() {
			processAfterChange(this, true);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Request(for=")
					.append(machine.getName());
			sb.append(";on=").append(powerOnBoards);
			sb.append(",off=").append(powerOffBoards);
			sb.append(",links=").append(linkRequests);
			return sb.append(")").toString();
		}
	}

	/**
	 * Describes a part of a request that modifies the power of an FPGA-managed
	 * inter-board link to be off.
	 *
	 * @author Donal Fellows
	 */
	private static final class Link {
		private final int board;

		private final Direction link;

		/**
		 * Create a request.
		 *
		 * @param board
		 *            The physical ID of the board that the FPGA is located on.
		 * @param link
		 *            Which link (and hence which FPGA).
		 * @param power
		 *            What state is the link to be put into?
		 */
		Link(int board, Direction link) {
			this.board = board;
			this.link = link;
		}

		@Override
		public String toString() {
			return "Link(" + board + "," + link + ":OFF)";
		}
	}

	/**
	 * Encapsulation of a connection.
	 */
	private abstract class AbstractSQL implements AutoCloseable {
		final Connection conn;

		AbstractSQL() throws SQLException {
			conn = db.getConnection();
		}

		void transaction(Transacted action) throws SQLException {
			DatabaseEngine.transaction(conn, action);
		}

		protected Query query(String sql) throws SQLException {
			return DatabaseEngine.query(conn, sql);
		}

		protected Query query(Resource sql) throws SQLException {
			return DatabaseEngine.query(conn, sql);
		}

		protected Update update(String sql) throws SQLException {
			return DatabaseEngine.update(conn, sql);
		}

		@Override
		public void close() throws SQLException {
			conn.close();
		}
	}

	/**
	 * Encapsulates several queries for {@link #takeRequests()}.
	 */
	private final class TakeReqsSQL extends AbstractSQL {
		private final Query getJobIdsWithChanges = query(getJobsWithChanges);

		private final Query getPowerChangesToDo = query(GET_CHANGES);

		private final Update setInProgress = update(SET_IN_PROGRESS);

		TakeReqsSQL() throws SQLException {
			// TODO can some of these be combined with RETURNING?
			// NB: Have to declare this constructor!
		}

		@Override
		public void close() throws SQLException {
			getJobIdsWithChanges.close();
			getPowerChangesToDo.close();
			setInProgress.close();
			super.close();
		}

		Iterable<Row> jobsWithChanges(Integer machineId) throws SQLException {
			return getJobIdsWithChanges.call(machineId);
		}

		Iterable<Row> getPowerChangesToDo(Integer jobId) throws SQLException {
			return getPowerChangesToDo.call(jobId);
		}

		int setInProgress(boolean inProgress, Integer changeId)
				throws SQLException {
			return setInProgress.call(inProgress, changeId);
		}
	}

	/**
	 * Copies out the requests for board power changes, marking them so that we
	 * remember they are being worked on.
	 *
	 * @return List of requests to pass to the {@link WorkerThread}s.
	 * @throws SQLException
	 *             If DB access fails
	 */
	private List<Request> takeRequests() throws SQLException {
		List<Request> requestCollector = new ArrayList<>();
		List<Machine> machines =
				new ArrayList<>(spallocCore.getMachines().values());
		try (TakeReqsSQL sql = new TakeReqsSQL()) {
			sql.transaction(() -> {
				// The outer loop is always over a small set, fortunately
				for (Machine machine : machines) {
					for (Row jobIds : sql.jobsWithChanges(machine.getId())) {
						takeRequestsForJob(machine, jobIds.getInteger("job_id"),
								sql, requestCollector);
					}
				}
			});
		}
		return requestCollector;
	}

	private void takeRequestsForJob(Machine machine, Integer jobId,
			TakeReqsSQL sql, List<Request> requestCollector)
			throws SQLException {
		List<Integer> changeIds = new ArrayList<>();
		List<Integer> boardsOn = new ArrayList<>();
		List<Integer> boardsOff = new ArrayList<>();
		List<Link> linksOff = new ArrayList<>();
		JobState from = UNKNOWN, to = UNKNOWN;

		for (Row row : sql.getPowerChangesToDo(jobId)) {
			changeIds.add(row.getInteger("change_id"));
			Integer board = row.getInteger("board_id");
			boolean switchOn = row.getBoolean("power");
			/*
			 * Set these multiple times; we don't care as they should be the
			 * same for each board.
			 */
			from = row.getEnum("from_state", JobState.class);
			to = row.getEnum("to_state", JobState.class);
			if (switchOn) {
				boardsOn.add(board);
			} else {
				boardsOff.add(board);
			}
			// Decode a collection of boolean columns
			for (Direction link : Direction.values()) {
				if (switchOn && !row.getBoolean(link.columnName)) {
					linksOff.add(new Link(board, link));
				}
			}
		}

		if (boardsOn.isEmpty() && boardsOff.isEmpty()) {
			// Nothing to do? Oh well, though this shouldn't be reachable...
			return;
		}

		requestCollector.add(new Request(machine, boardsOn, boardsOff, linksOff,
				jobId, from, to, changeIds));
		for (Integer changeId : changeIds) {
			sql.setInProgress(true, changeId);
		}
	}

	/**
	 * The profile of {@linkplain Update updates} for
	 * {@code processAfterChange()}.
	 */
	private final class AfterSQL extends AbstractSQL {
		private final Update setBoardState = update(SET_BOARD_POWER);

		private final Update setJobState = update(SET_STATE_PENDING);

		private final Update setInProgress = update(SET_IN_PROGRESS);

		private final Update deallocateBoards = update(DEALLOCATE_BOARDS_JOB);

		private final Update deleteChange = update(FINISHED_PENDING);

		AfterSQL() throws SQLException {
		}

		@Override
		public void close() throws SQLException {
			deleteChange.close();
			deallocateBoards.close();
			setInProgress.close();
			setJobState.close();
			setBoardState.close();
			super.close();
		}

		// What follows are type-safe wrappers

		int setBoardState(boolean state, Integer boardId) throws SQLException {
			return setBoardState.call(state, boardId);
		}

		int setJobState(JobState state, int pending, Integer jobId)
				throws SQLException {
			return setJobState.call(state, pending, jobId);
		}

		int setInProgress(boolean progress, Integer changeId)
				throws SQLException {
			return setInProgress.call(progress, changeId);
		}

		int deallocateBoards(Integer jobId) throws SQLException {
			return deallocateBoards.call(jobId);
		}

		int deleteChange(Integer changeId) throws SQLException {
			return deleteChange.call(changeId);
		}
	}

	/**
	 * Handles what happens after a set of changes to a BMP complete,
	 * successfully or not. Note that this happens on the BMP worker thread.
	 *
	 * @param request
	 *            What did we tell the BMP to do?
	 * @param fail
	 *            Was this a failure? On failure, we roll back the job state to
	 *            what it was before. On success we move to the state it
	 *            supposed to be in.
	 */
	private void processAfterChange(Request request, boolean fail) {
		try (AfterSQL sql = new AfterSQL()) {
			sql.transaction(() -> processAfterChange(request, fail, sql));
		} catch (SQLException e) {
			log.error("problem with database", e);
		} finally {
			epochs.nextJobsEpoch();
			epochs.nextMachineEpoch();
		}
	}

	/**
	 * Handles the database changes after a set of changes to a BMP complete,
	 * successfully or not.
	 *
	 * @param request
	 *            What did we tell the BMP to do?
	 * @param fail
	 *            Was this a failure? On failure, we roll back the job state to
	 *            what it was before. On success we move to the state it
	 *            supposed to be in.
	 * @param sql
	 *            How to access the DB
	 * @throws SQLException
	 *             if something goes badly wrong
	 */
	private void processAfterChange(Request request, boolean failed,
			AfterSQL sql) throws SQLException {
		int turnedOn = 0, turnedOff = 0, jobChange = 0, moved = 0,
				deallocated = 0, killed = 0;
		if (failed) {
			for (Integer changeId : request.changeIds) {
				moved += sql.setInProgress(false, changeId);
			}
			jobChange += sql.setJobState(request.from, 0, request.jobId);
			// TODO what else should happen here?
		} else {
			for (Integer board : request.powerOnBoards) {
				turnedOn += sql.setBoardState(true, board);
			}
			for (Integer board : request.powerOffBoards) {
				turnedOff += sql.setBoardState(false, board);
			}
			jobChange += sql.setJobState(request.to, 0, request.jobId);
			if (request.to == DESTROYED) {
				/*
				 * Need to mark the boards as not allocated; can't do that until
				 * they've been switched off.
				 */
				deallocated += sql.deallocateBoards(request.jobId);
			}
			for (Integer changeId : request.changeIds) {
				killed += sql.deleteChange(changeId);
			}
		}
		log.debug(
				"post-switch ({}:{}->{}): up:{} down:{} jobChanges:{} "
						+ "inProgress:{} deallocated:{} bmpTasksDone:{}",
				request.jobId, request.from, request.to, turnedOn, turnedOff,
				jobChange, moved, deallocated, killed);
	}

	private void addRequestToBMPQueue(Request request)
			throws IOException, SpinnmanException, SQLException,
			InterruptedException {
		requireNonNull(request, "request must not be null");
		/*
		 * Ensure that the transceiver for the machine exists while we're still
		 * in the current thread; the connection inside Machine inside Request
		 * is _not_ safe to hand off between threads. Fortunately, the worker
		 * doesn't need that... provided we get the transceiver now.
		 */
		txrxFactory.getTransciever(request.machine);
		WorkerState ws = getWorkerState(request.machine);
		ws.requests.add(request);
		synchronized (this) {
			if (!ws.requestsPending) {
				ws.requestsPending = true;
			}
			notifyAll();
		}
	}

	/**
	 * Get the worker state for talking to a machine's BMPs. If necessary,
	 * initialise a worker and the state record used to communicate with it.
	 *
	 * @param machine
	 *            The machine that the worker handles
	 * @return The worker state record.
	 * @throws InterruptedException
	 *             If we're interrupted during worker launch.
	 */
	private WorkerState getWorkerState(Machine machine)
			throws InterruptedException {
		synchronized (state) {
			WorkerState ws = state.get(machine);
			if (isNull(ws)) {
				ws = new WorkerState(machine);
				state.put(machine, ws);
			}
			if (isNull(ws.workerThread)) {
				WorkerState ws2 = ws;
				executor.execute(() -> backgroundThread(ws2));
				while (isNull(ws.workerThread)) {
					state.wait();
				}
			}
			return ws;
		}
	}

	// ----------------------------------------------------------------
	// WORKER IMPLEMENTATION

	/** The state of worker threads that can be seen outside the thread. */
	private static class WorkerState {
		/** What machine is the worker handling? */
		final Machine machine;

		/** Queue of requests to the machine to carry out. */
		final Queue<Request> requests = new ConcurrentLinkedDeque<>();

		/** Whether there are any requests pending. */
		boolean requestsPending = false;

		/** What thread is serving as the worker? */
		Thread workerThread;

		WorkerState(Machine machine) {
			this.machine = machine;
		}

		void interrupt() {
			Thread wt = workerThread;
			if (nonNull(wt)) {
				wt.interrupt();
			}
		}
	}

	@PreDestroy
	private void shutDownWorkers() throws InterruptedException {
		markAllForStop();
		executor.shutdown();
		synchronized (state) {
			for (WorkerState ws : state.values()) {
				ws.interrupt();
			}
		}
		executor.awaitTermination(SECONDS_BETWEEN_TRIES, SECONDS);
		group.interrupt();
	}

	/**
	 * Establishes (while active) that the current thread is a worker thread for
	 * handling a machine's communications.
	 *
	 * @author Donal Fellows
	 */
	private final class BindWorker implements AutoCloseable {
		private final WorkerState ws;

		private BindWorker(WorkerState ws, String name) {
			this.ws = ws;
			currentThread().setName("bmp-worker:" + name);
			synchronized (state) {
				ws.workerThread = currentThread();
				state.notifyAll();
			}
		}

		@Override
		public void close() throws Exception {
			synchronized (state) {
				ws.workerThread = null;
			}
			currentThread().setName("bmp-worker:[unbound]");
		}
	}

	private synchronized void waitForPending(WorkerState ws)
			throws InterruptedException {
		while (!ws.requestsPending) {
			wait();
		}
	}

	private synchronized boolean shouldTerminate(WorkerState ws) {
		if (ws.requests.isEmpty()) {
			ws.requestsPending = false;
			notifyAll();

			if (stop) {
				return true;
			}
		}
		return false;
	}

	private synchronized void markAllForStop() {
		stop = true;
		notifyAll();
	}

	/**
	 * The background thread for interacting with the BMP.
	 *
	 * @param ws
	 *            What SpiNNaker machine is this thread servicing?
	 */
	void backgroundThread(WorkerState ws) {
		String name = ws.machine.getName();
		try (MDCCloseable mdc = putCloseable("machine", name);
				BindWorker binding = new BindWorker(ws, name)) {
			do {
				waitForPending(ws);

				/*
				 * No lock needed; this is the only thread that removes from
				 * this queue.
				 */
				if (!ws.requests.isEmpty()) {
					processRequest(ws.requests.peek());
					ws.requests.remove();
				}

				/*
				 * If nothing left in the queues, clear the request flag and
				 * break out of queue-processing loop.
				 */
			} while (!shouldTerminate(ws));
		} catch (InterruptedException e) {
			// Thread is being shut down
			markAllForStop();
			log.info("worker thread '{}' was interrupted",
					currentThread().getName());
		} catch (Exception e) {
			/*
			 * If the thread crashes something has gone wrong with this program
			 * (not the machine), setting stop will cause setPower and
			 * setLinkEnable to fail, hopefully propagating news of this crash.
			 */
			markAllForStop();
			log.error("unhandled exception for '{}'", currentThread().getName(),
					e);
		}
	}

	private void processRequest(Request request) throws InterruptedException {
		BMPTransceiverInterface txrx;
		try {
			txrx = txrxFactory.getTransciever(request.machine);
		} catch (IOException | SpinnmanException | SQLException e) {
			// Shouldn't ever happen; the transceiver ought to be pre-built
			log.error("could not get transceiver", e);
			return;
		}

		try (MDCCloseable mdc = putCloseable("changes",
				asList(request.powerOnBoards.size(),
						request.powerOffBoards.size(),
						request.linkRequests.size()).toString())) {
			for (int nTries = 0; nTries++ < N_REQUEST_TRIES;) {
				if (tryChangePowerState(request, txrx,
						nTries == N_REQUEST_TRIES)) {
					break;
				}
				sleep(SECONDS_BETWEEN_TRIES * MS_PER_S);
			}
		}
	}

	private boolean tryChangePowerState(Request request,
			BMPTransceiverInterface txrx, boolean isLastTry)
			throws InterruptedException {
		Machine machine = request.machine;
		try {
			changeBoardPowerState(txrx, request);
			request.done();
			// Exit the retry loop (in caller) if the requests all worked
			return true;
		} catch (InterruptedException e) {
			/*
			 * We were interrupted! This happens when we're shutting down. Log
			 * (because we're in an inconsistent state) and rethrow so that the
			 * outside gets to clean up.
			 */
			log.error("Requests failed on BMP {} because of interruption",
					machine, e);
			request.failed();
			currentThread().interrupt();
			throw e;
		} catch (Exception e) {
			if (!isLastTry) {
				/*
				 * Log somewhat gently; we *might* be able to recover...
				 */
				log.warn("Retrying requests on BMP {} after {} seconds: {}",
						machine, SECONDS_BETWEEN_TRIES, e.getMessage());
				// Ask for a retry
				return false;
			}
			log.error("Requests failed on BMP {}", machine, e);
			request.failed();
			// This is (probably) a permanent failure; stop retry loop
			return true;
		}
	}
}
