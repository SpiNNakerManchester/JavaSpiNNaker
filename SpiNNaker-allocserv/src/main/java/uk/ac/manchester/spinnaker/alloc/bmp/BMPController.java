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
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static org.sqlite.SQLiteErrorCode.SQLITE_BUSY;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.UNKNOWN;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.OFF;
import static uk.ac.manchester.spinnaker.messages.model.FPGAMainRegisters.BASE_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.model.FPGAMainRegisters.FLAG;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_OFF;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_ON;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;

import javax.annotation.PreDestroy;

import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.sqlite.SQLiteException;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;
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

	private static final Collection<Integer> FPGA_IDS =
			unmodifiableCollection(asList(0, 1, 2));

	private static final long INTER_TAKE_DELAY = 10000;

	private volatile boolean stop;

	private Map<Machine, Thread> workers = new HashMap<>();

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

	private ExecutorService executor = newCachedThreadPool(WorkerThread::new);

	private class WorkerThread extends Thread {
		WorkerThread(Runnable r) {
			super(r, "bmp-worker");
		}
	}

	/** The state of worker threads that can be seen outside the thread. */
	private static class WorkerState {
		/** What machine is the worker handling? */
		final Machine machine;

		/** Queue of requests to the machine to carry out. */
		final ConcurrentLinkedDeque<Request> requests;

		/** Whether there are any requests pending. */
		boolean requestsPending;

		WorkerState(Machine machine) {
			this.machine = machine;
			requests = new ConcurrentLinkedDeque<>();
		}
	}

	@PreDestroy
	private void shutDownWorkers() throws InterruptedException {
		synchronized (this) {
			stop = true;
			notifyAll();
		}
		executor.shutdown();
		synchronized (workers) {
			for (Thread worker : workers.values()) {
				if (worker instanceof WorkerThread) {
					worker.interrupt();
				}
			}
		}
		executor.awaitTermination(SECONDS_BETWEEN_TRIES, SECONDS);
	}

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
	public void processRequests()
			throws SQLException, IOException, SpinnmanException,
			InterruptedException {
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

	private boolean isGoodFPGA(Machine machine, BMPTransceiverInterface txrx,
			int board, int fpga) throws ProcessException, IOException {
		// FPGA ID is bottom two bits of FLAG register in main register bank
		int fpgaId = txrx.readFPGARegister(fpga, BASE_ADDRESS + FLAG.offset, 0,
				0, board) & FPGA_FLAG_ID_MASK;
		boolean ok = fpgaId == fpga;
		if (!ok) {
			log.warn("FPGA {} on board {} of {} has incorrect FPGA ID flag {}",
					fpga, board, machine.getName(), fpgaId);
		}
		return ok;
	}

	private void setLinkState(BMPTransceiverInterface txrx, int board,
			Direction link, PowerState power)
			throws ProcessException, IOException {
		// skip FPGA link configuration if old BMP version
		VersionInfo vi = txrx.readBMPVersion(0, 0, board);
		if (vi.versionNumber.majorVersion < BMP_VERSION_MIN) {
			return;
		}
		txrx.writeFPGARegister(link.fpga, link.addr,
				power == PowerState.ON ? 0 : 1, new BMPCoords(0, 0), board);
	}

	private void powerOnAndCheck(Machine machine, BMPTransceiverInterface txrx,
			List<Integer> boards)
			throws ProcessException, InterruptedException, IOException {
		List<Integer> boardsToPower = boards;
		for (int attempt = 1; attempt <= NUM_FPGA_RETRIES; attempt++) {
			txrx.power(POWER_ON, new BMPCoords(0, 0), boardsToPower);
			List<Integer> retryBoards = new ArrayList<>();
			for (int board : boardsToPower) {
				// Skip board if old BMP version
				VersionInfo v = txrx.readBMPVersion(0, 0, board);
				if (v.versionNumber.majorVersion < BMP_VERSION_MIN) {
					continue;
				}

				for (int fpga : FPGA_IDS) {
					if (!isGoodFPGA(machine, txrx, board, fpga)) {
						retryBoards.add(board);
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
		throw new RuntimeException("Could not get correct FPGA ID after "
				+ NUM_FPGA_RETRIES + " tries");
	}

	/**
	 * Describes a request to modify the power status of a collection of boards.
	 * The boards must be on a single machine and must all be assigned to a
	 * single job.
	 *
	 * @author Donal Fellows
	 */
	public final class Request {
		private final RequestChange change;

		private int jobId;

		private JobState from;

		private JobState to;

		private List<Integer> changeIds;

		/**
		 * Create a request.
		 *
		 * @param requestedChange
		 *            What change do we want to apply?
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
		Request(RequestChange requestedChange, int jobId, JobState from,
				JobState to, List<Integer> changeIds) {
			this.change = requireNonNull(requestedChange);
			this.jobId = jobId;
			this.from = from;
			this.to = to;
			this.changeIds = changeIds;
		}

		/**
		 * Declare this to be a successful request.
		 */
		private void done() {
			processAfterChange(jobId, from, to, change, changeIds, false);
		}

		/**
		 * Declare this to be a failed request.
		 */
		private void failed() {
			processAfterChange(jobId, from, to, change, changeIds, true);
		}
	}

	/**
	 * Describes the detail of a request to modify the power status of a
	 * collection of boards. The boards must be on a single machine.
	 *
	 * @author Donal Fellows
	 */
	public static class RequestChange {
		private final Machine machine;

		private final List<Integer> powerOnBoards;

		private final List<Integer> powerOffBoards;

		private final List<LinkRequest> linkRequests;

		/**
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
		 */
		RequestChange(Machine machine, List<Integer> powerOnBoards,
				List<Integer> powerOffBoards, List<LinkRequest> linkRequests) {
			this.machine = requireNonNull(machine);
			this.powerOnBoards = new ArrayList<>(
					isNull(powerOnBoards) ? emptyList() : powerOnBoards);
			this.powerOffBoards = new ArrayList<>(
					isNull(powerOffBoards) ? emptyList() : powerOffBoards);
			this.linkRequests = new ArrayList<>(
					isNull(linkRequests) ? emptyList() : linkRequests);
		}
	}

	/**
	 * Describes a part of a request that modifies the power of an FPGA-managed
	 * inter-board link.
	 *
	 * @author Donal Fellows
	 */
	public static class LinkRequest {
		private final int board;

		private final Direction link;

		private final PowerState power;

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
		public LinkRequest(int board, Direction link, PowerState power) {
			this.board = board;
			this.link = link;
			this.power = power;
		}

		@Override
		public int hashCode() {
			return board ^ link.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof LinkRequest) {
				LinkRequest lr = (LinkRequest) other;
				return board == lr.board && link == lr.link;
			}
			return false;
		}
	}

	private void processRequest(Request request) throws InterruptedException {
		BMPTransceiverInterface txrx;
		try {
			txrx = txrxFactory.getTransciever(request.change.machine);
		} catch (IOException | SpinnmanException | SQLException e) {
			log.error("could not get transceiver", e);
			return;
		}
		MDC.put("changes",
				asList(request.change.powerOnBoards.size(),
						request.change.powerOffBoards.size(),
						request.change.linkRequests.size()));
		try {
			for (int nTries = 0; nTries++ < N_REQUEST_TRIES;) {
				if (tryChangePowerState(request, txrx,
						nTries == N_REQUEST_TRIES)) {
					break;
				}
			}
		} finally {
			MDC.remove("changes");
		}
	}

	private boolean tryChangePowerState(Request request,
			BMPTransceiverInterface txrx, boolean isLastTry)
			throws InterruptedException {
		try {
			changeBoardPowerState(txrx, request);
			return true;
		} catch (InterruptedException e) {
			log.error("Requests failed on BMP {}", request.change.machine, e);
			request.failed();
			currentThread().interrupt();
			throw e;
		} catch (Exception e) {
			if (!isLastTry && !(e instanceof InterruptedException)) {
				log.error("Retrying requests on BMP {} after {} seconds: {}",
						request.change.machine, SECONDS_BETWEEN_TRIES,
						e.getMessage());
				sleep(SECONDS_BETWEEN_TRIES * MS_PER_S);
				return false;
			}
			log.error("Requests failed on BMP {}", request.change.machine, e);
			request.failed();
			return true;
		}
	}

	/**
	 * Change the power state of a board.
	 *
	 * @param txrx
	 *            The transceiver to use
	 * @param request
	 *            The request to carry out
	 * @throws ProcessException
	 *             If the transceiver chokes
	 * @throws InterruptedException
	 *             If interrupted
	 * @throws IOException
	 *             If network I/O fails
	 */
	private void changeBoardPowerState(BMPTransceiverInterface txrx,
			Request request)
			throws ProcessException, InterruptedException, IOException {
		RequestChange change = request.change;
		// Send any power on commands
		if (!change.powerOnBoards.isEmpty()) {
			powerOnAndCheck(change.machine, txrx, change.powerOnBoards);
		}

		// Process link requests next
		for (LinkRequest linkReq : change.linkRequests) {
			// Set the link state, as required
			setLinkState(txrx, linkReq.board, linkReq.link, linkReq.power);
		}

		// Finally send any power off commands
		if (!change.powerOffBoards.isEmpty()) {
			txrx.power(POWER_OFF, 0, 0, change.powerOffBoards);
		}

		// Exit the retry loop if the requests all worked
		request.done();
	}

	private final class TakeReqsSQL implements AutoCloseable {
		private final Query getJobIds;

		private final Query getPowerChangesToDo;

		private final Update setInProgress;

		TakeReqsSQL(Connection conn) throws SQLException {
			getJobIds = query(conn, getJobsWithChanges);
			getPowerChangesToDo = query(conn, GET_CHANGES);
			setInProgress = update(conn, SET_IN_PROGRESS);
			// TODO can some of these be combined with RETURNING?
		}

		@Override
		public void close() throws SQLException {
			getJobIds.close();
			getPowerChangesToDo.close();
			setInProgress.close();
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
		List<Request> requests = new ArrayList<>();
		try (Connection conn = db.getConnection();
				TakeReqsSQL sql = new TakeReqsSQL(conn)) {
			transaction(conn, () -> {
				for (Machine machine : spallocCore.getMachines().values()) {
					for (Row jobIds : sql.getJobIds.call(machine.getId())) {
						takeRequestsForJob(requests, sql, machine,
								jobIds.getInt("job_id"));
					}
				}
			});
		}
		return requests;
	}

	private void takeRequestsForJob(List<Request> requests, TakeReqsSQL sql,
			Machine machine, int jobId) throws SQLException {
		List<Integer> changeIds = new ArrayList<>();
		List<Integer> boardsOn = new ArrayList<>();
		List<Integer> boardsOff = new ArrayList<>();
		List<LinkRequest> linksOff = new ArrayList<>();
		JobState from = UNKNOWN, to = UNKNOWN;
		for (Row row : sql.getPowerChangesToDo.call(jobId)) {
			changeIds.add(row.getInt("change_id"));
			int board = row.getInt("board_id");
			boolean switchOn = row.getBoolean("power");
			from = row.getEnum("from_state", JobState.class);
			to = row.getEnum("to_state", JobState.class);
			if (switchOn) {
				boardsOn.add(board);
			} else {
				boardsOff.add(board);
			}
			for (Direction link : Direction.values()) {
				if (switchOn && !row.getBoolean(link.columnName)) {
					linksOff.add(new LinkRequest(board, link, OFF));
				}
			}
		}
		requests.add(new Request(
				new RequestChange(machine, boardsOn, boardsOff, linksOff),
				jobId, from, to, changeIds));
		for (int changeId : changeIds) {
			sql.setInProgress.call(true, changeId);
		}
	}

	/**
	 * The profile of {@linkplain Update updates} for {@code doneRequest()}.
	 */
	private static final class DoneUpdates implements AutoCloseable {
		private final Update setBoardState;

		private final Update setJobState;

		private final Update setInProgress;

		private final Update deallocateBoards;

		private final Update deleteChange;

		DoneUpdates(Connection conn) throws SQLException {
			setBoardState = update(conn, SET_BOARD_POWER);
			setJobState = update(conn, SET_STATE_PENDING);
			setInProgress = update(conn, SET_IN_PROGRESS);
			deallocateBoards = update(conn, DEALLOCATE_BOARDS_JOB);
			deleteChange = update(conn, FINISHED_PENDING);
		}

		@Override
		public void close() throws SQLException {
			deleteChange.close();
			deallocateBoards.close();
			setInProgress.close();
			setJobState.close();
			setBoardState.close();
		}

		// What follows are type-safe wrappers

		int setBoardState(boolean state, int jobId) throws SQLException {
			return setBoardState.call(state, jobId);
		}

		int setJobState(JobState state, int pending, int jobId)
				throws SQLException {
			return setJobState.call(state, pending, jobId);
		}

		int setInProgress(boolean progress, int changeId) throws SQLException {
			return setInProgress.call(progress, changeId);
		}

		int deallocateBoards(int jobId) throws SQLException {
			return deallocateBoards.call(jobId);
		}

		int deleteChange(int changeId) throws SQLException {
			return deleteChange.call(changeId);
		}
	}

	/**
	 * Handles what happens after a set of changes to a BMP complete,
	 * successfully or not. Note that this happens on the BMP worker thread.
	 *
	 * @param jobId
	 *            What job is this for?
	 * @param fromState
	 *            What state was the job coming from (it's currently in the
	 *            {@link JobState#POWER} state).
	 * @param toState
	 *            What state is the job supposed to go to.
	 * @param change
	 *            What did we tell the BMP to do?
	 * @param changeIds
	 *            What database records did we get the instructions from?
	 * @param fail
	 *            Was this a failure? On failure, we roll back the job state to
	 *            what it was before. On success we move to the state it
	 *            supposed to be in.
	 */
	private void processAfterChange(int jobId, JobState fromState,
			JobState toState, RequestChange change, List<Integer> changeIds,
			boolean fail) {
		try (Connection conn = db.getConnection();
				DoneUpdates sql = new DoneUpdates(conn)) {
			transaction(conn, () -> processAfterChange(jobId, fromState,
					toState, change, changeIds, fail, sql));
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
	 * @param jobId
	 *            What job is this for?
	 * @param fromState
	 *            What state was the job coming from (it's currently in the
	 *            {@link JobState#POWER} state).
	 * @param toState
	 *            What state is the job supposed to go to.
	 * @param change
	 *            What did we tell the BMP to do?
	 * @param changeIds
	 *            What database records did we get the instructions from?
	 * @param fail
	 *            Was this a failure? On failure, we roll back the job state to
	 *            what it was before. On success we move to the state it
	 *            supposed to be in.
	 * @param sql
	 *            How to access the DB
	 * @throws SQLException
	 *             if something goes badly wrong
	 */
	private void processAfterChange(int jobId, JobState fromState,
			JobState toState, RequestChange change, List<Integer> changeIds,
			boolean failed, DoneUpdates sql) throws SQLException {
		int turnedOn = 0, turnedOff = 0, jobChange = 0, moved = 0,
				deallocated = 0, killed = 0;
		if (failed) {
			for (int changeId : changeIds) {
				moved += sql.setInProgress(false, changeId);
			}
			jobChange += sql.setJobState(fromState, 0, jobId);
			// TODO what else should happen here?
		} else {
			for (int board : change.powerOnBoards) {
				turnedOn += sql.setBoardState(true, board);
			}
			for (int board : change.powerOffBoards) {
				turnedOff += sql.setBoardState(false, board);
			}
			jobChange += sql.setJobState(toState, 0, jobId);
			if (toState == DESTROYED) {
				/*
				 * Need to mark the boards as not allocated; can't do that until
				 * they've been switched off.
				 */
				deallocated += sql.deallocateBoards(jobId);
			}
			for (int changeId : changeIds) {
				killed += sql.deleteChange(changeId);
			}
		}
		log.debug(
				"post-switch ({}:{}->{}): up:{} down:{} jobChanges:{} "
						+ "inProgress:{} deallocated:{} bmpTasksDone:{}",
				jobId, fromState, toState, turnedOn, turnedOff, jobChange,
				moved, deallocated, killed);
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
		Machine m = request.change.machine;
		txrxFactory.getTransciever(m);
		WorkerState ws = getWorkerState(m);
		createWorkerIfRequired(ws);
		ws.requests.addLast(request);
		synchronized (this) {
			if (!ws.requestsPending) {
				ws.requestsPending = true;
			}
			notifyAll();
		}
	}

	private synchronized WorkerState getWorkerState(Machine m) {
		return state.computeIfAbsent(m, WorkerState::new);
	}

	private void createWorkerIfRequired(WorkerState ws)
			throws InterruptedException {
		synchronized (workers) {
			if (!workers.containsKey(ws.machine)) {
				executor.execute(() -> backgroundThread(ws));
				while (workers.get(ws.machine) == null) {
					workers.wait();
				}
			}
		}
	}

	/**
	 * Establishes (while active) that the current thread is a worker thread for
	 * handling a machine's communications.
	 *
	 * @author Donal Fellows
	 */
	private final class BindWorker implements AutoCloseable {
		private final Machine machine;

		private BindWorker(Machine machine) {
			synchronized (workers) {
				this.machine = machine;
				workers.put(machine, currentThread());
				workers.notifyAll();
			}
		}

		@Override
		public void close() throws Exception {
			synchronized (workers) {
				workers.remove(machine);
			}
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
	}

	/**
	 * The background thread for interacting with the BMP.
	 *
	 * @param ws
	 *            What SpiNNaker machine is this thread servicing?
	 */
	void backgroundThread(WorkerState ws) {
		MDC.put("machine", ws.machine.getName());
		try (AutoCloseable binding = new BindWorker(ws.machine)) {
			while (true) {
				waitForPending(ws);

				/*
				 * No lock needed; this is the only thread that removes from
				 * this queue.
				 */
				if (!ws.requests.isEmpty()) {
					processRequest(ws.requests.peekFirst());
					ws.requests.removeFirst();
				}

				/*
				 * If nothing left in the queues, clear the request flag and
				 * break out of queue-processing loop.
				 */
				if (shouldTerminate(ws)) {
					// If we've been told to stop, actually stop the thread now
					return;
				}
			}
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
}
