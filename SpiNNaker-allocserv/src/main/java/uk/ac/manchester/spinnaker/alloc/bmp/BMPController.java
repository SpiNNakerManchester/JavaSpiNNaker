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
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.OFF;
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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/**
 * Manages the BMPs of machines controlled by Spalloc.
 *
 * @author Donal Fellows
 */
@Service("bmpController")
@ManagedResource("Spalloc:type=BMPController,name=bmpController")
public class BMPController extends SQLQueries {
	private static final Logger log = getLogger(BMPController.class);

	private static final int FPGA_FLAG_REGISTER_ADDRESS = 0x40004;

	private static final int FPGA_FLAG_ID_MASK = 0x3;

	private static final int NUM_FPGA_RETRIES = 3;

	private static final int BMP_VERSION_MIN = 2;

	private static final int SECONDS_BETWEEN_TRIES = 15;

	private static final int N_REQUEST_TRIES = 2;

	private static final int MS_PER_S = 1000;

	private static final Collection<Integer> FPGA_IDS =
			unmodifiableCollection(asList(0, 1, 2));

	private static final long INTER_TAKE_DELAY = 10000;

	private boolean requestsPending;

	private volatile boolean stop;

	private ConcurrentLinkedDeque<Request> requests;

	private Runnable onThreadStart;

	private Map<Machine, Thread> workers = new HashMap<>();

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private SpallocAPI spallocCore;

	@Autowired
	private ServiceMasterControl serviceControl;

	@Autowired
	private TransceiverFactory txrxFactory;

	private Collection<Machine> machines;

	@PostConstruct
	private void discoverMachines() throws SQLException {
		machines = spallocCore.getMachines().values();
	}

	private ExecutorService executor = newCachedThreadPool(WorkerThread::new);

	class WorkerThread extends Thread {
		WorkerThread(Runnable r) {
			super(r, "bmp-worker");
		}
	}

	@PreDestroy
	private void shutDownWorkers() throws InterruptedException {
		stop = true;
		synchronized (this) {
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

		for (Request req : takeRequests()) {
			addRequest(req);
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
	@ManagedAttribute(description = "An estimate of the number of requests "
			+ "pending.")
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
		return requests.size();
	}

	private boolean isGoodFPGA(Machine machine, Transceiver txrx, int board,
			int fpga) throws ProcessException, IOException {
		int fpgaId = txrx.readFPGARegister(fpga, FPGA_FLAG_REGISTER_ADDRESS, 0,
				0, board);
		boolean ok = (fpgaId & FPGA_FLAG_ID_MASK) == fpga;
		if (!ok) {
			log.warn("FPGA {} on board {} of {} has incorrect FPGA ID flag {}",
					fpga, board, machine.getName(), fpgaId & FPGA_FLAG_ID_MASK);
		}
		return ok;
	}

	private void setLinkState(Transceiver txrx, int board, Direction link,
			PowerState power) throws ProcessException, IOException {
		// skip FPGA link configuration if old BMP version
		VersionInfo vi = txrx.readBMPVersion(0, 0, board);
		if (vi.versionNumber.majorVersion < BMP_VERSION_MIN) {
			return;
		}
		txrx.writeFPGARegister(link.fpga, link.addr,
				power == PowerState.ON ? 0 : 1, new BMPCoords(0, 0), board);
	}

	private void powerOnAndCheck(Machine machine, Transceiver txrx,
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
	 * The boards must be on a single machine and should all be assigned to a
	 * single job.
	 *
	 * @author Donal Fellows
	 */
	public static class Request {
		private final RequestChange change;

		private final OnDone onDone;

		/**
		 * Create a request.
		 *
		 * @param requestedChange
		 *            What change do we want to apply?
		 * @param onDone
		 *            An optional callback for when the changes are fully
		 *            processed (whether successfully or not). May be
		 *            {@code null} if there is no callback.
		 */
		public Request(RequestChange requestedChange, OnDone onDone) {
			this.change = requireNonNull(requestedChange);
			this.onDone = onDone;
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
		 *            {@code null}.
		 * @param powerOffBoards
		 *            What boards (by physical ID) are to be powered off? May be
		 *            {@code null}.
		 * @param linkRequests
		 *            Any link power control requests. By default, links are on
		 *            if their board is on and they are connected; it is
		 *            <em>useful and relevant</em> to modify the power state of
		 *            links on the periphery of an allocation. May be
		 *            {@code null}.
		 */
		RequestChange(Machine machine, List<Integer> powerOnBoards,
				List<Integer> powerOffBoards, List<LinkRequest> linkRequests) {
			this.machine = requireNonNull(machine);
			this.powerOnBoards = new ArrayList<>(
					powerOnBoards == null ? emptyList() : powerOnBoards);
			this.powerOffBoards = new ArrayList<>(
					powerOffBoards == null ? emptyList() : powerOffBoards);
			this.linkRequests = new ArrayList<>(
					linkRequests == null ? emptyList() : linkRequests);
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

	/**
	 * A callback handler that can be used to notify code that a BMP request has
	 * completed. Note that the callback will be processed on a BMP controller
	 * worker thread.
	 *
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	public interface OnDone {
		/**
		 * The callback.
		 *
		 * @param failureReason
		 *            Will be {@code null} on success. If the request failed,
		 *            summary information about why.
		 * @param exception
		 *            The details of the reason for the failure. {@code null} on
		 *            success. Note that the exception <em>will</em> have been
		 *            logged if it is appropriate to do so; callbacks should not
		 *            log.
		 */
		void call(String failureReason, Exception exception);
	}

	private void processRequest(Request request) throws InterruptedException {
		Transceiver txrx;
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
				try {
					// Send any power on commands
					if (!request.change.powerOnBoards.isEmpty()) {
						powerOnAndCheck(request.change.machine, txrx,
								request.change.powerOnBoards);
					}

					// Process link requests next
					for (LinkRequest linkReq : request.change.linkRequests) {
						// Set the link state, as required
						setLinkState(txrx, linkReq.board, linkReq.link,
								linkReq.power);
					}

					// Finally send any power off commands
					if (!request.change.powerOffBoards.isEmpty()) {
						txrx.power(POWER_OFF, 0, 0,
								request.change.powerOffBoards);
					}

					// Exit the retry loop if the requests all worked
					if (request.onDone != null) {
						request.onDone.call(null, null);
					}
					break;
				} catch (InterruptedException e) {
					String reason =
							"Requests failed on BMP " + request.change.machine;
					log.error(reason, e);
					if (request.onDone != null) {
						request.onDone.call(reason, e);
					}
					throw e;
				} catch (Exception e) {
					if (nTries == N_REQUEST_TRIES) {
						String reason =
								"Requests failed on BMP "
										+ request.change.machine;
						log.error(reason, e);
						if (request.onDone != null) {
							request.onDone.call(reason, e);
						}
						currentThread().interrupt();
						break;
					}
					log.error(
							"Retrying requests on BMP {} after {} seconds: {}",
							request.change.machine, SECONDS_BETWEEN_TRIES,
							e.getMessage());
					sleep(SECONDS_BETWEEN_TRIES * MS_PER_S);
				}
			}
		} finally {
			MDC.remove("changes");
		}
	}

	List<Request> takeRequests() throws SQLException {
		List<Request> requests = new ArrayList<>();
		try (Connection conn = db.getConnection();
				Query getJobIds = query(conn, getJobsWithChanges);
				Query getChanges = query(conn, GET_CHANGES);
				Update setInProgress = update(conn, SET_IN_PROGRESS)) {
			transaction(conn, () -> {
				for (Machine machine : machines) {
					for (Row jobIds : getJobIds.call(machine.getId())) {
						takeRequestsForJob(requests, getChanges, setInProgress,
								machine, jobIds.getInt("job_id"));
					}
				}
			});
		}
		return requests;
	}

	private void takeRequestsForJob(List<Request> requests, Query getChanges,
			Update setInProgress, Machine machine, int jobId)
			throws SQLException {
		List<Integer> changeIds = new ArrayList<>();
		List<Integer> boardsOn = new ArrayList<>();
		List<Integer> boardsOff = new ArrayList<>();
		List<LinkRequest> linksOff = new ArrayList<>();
		JobState[] states = new JobState[2];
		for (Row row : getChanges.call(jobId)) {
			changeIds.add(row.getInt("change_id"));
			int board = row.getInt("board_id");
			boolean switchOn = row.getBoolean("power");
			states[0] = row.getEnum("from_state", JobState.class);
			states[1] = row.getEnum("to_state", JobState.class);
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
		RequestChange change =
				new RequestChange(machine, boardsOn, boardsOff, linksOff);
		requests.add(new Request(change, (fail, exn) -> doneRequest(jobId,
				states[0], states[1], change, changeIds, fail, exn)));
		for (int changeId : changeIds) {
			setInProgress.call(true, changeId);
		}
	}

	void doneRequest(int jobId, JobState fromState, JobState toState,
			RequestChange change, List<Integer> changeIds, String fail,
			Exception exn) {
		if (fail != null) {
			log.error("failed to set power on BMPs: {}", fail, exn);
		}

		try (Connection conn = db.getConnection();
				Update setBoardState = update(conn, SET_BOARD_POWER);
				Update deleteChange = update(conn, FINISHED_PENDING);
				Update setJobState = update(conn, SET_STATE_PENDING);
				Update setInProgress = update(conn, SET_IN_PROGRESS)) {
			transaction(conn, () -> {
				if (fail != null) {
					for (int changeId : changeIds) {
						setInProgress.call(false, changeId);
					}
					setJobState.call(fromState, 0, jobId);
				} else {
					for (int board : change.powerOnBoards) {
						setBoardState.call(true, board);
					}
					for (int board : change.powerOffBoards) {
						setBoardState.call(false, board);
					}
					setJobState.call(toState, 0, jobId);
				}
				for (int changeId : changeIds) {
					deleteChange.call(changeId);
				}
			});
		} catch (SQLException e) {
			log.error("problem with database", e);
		}
	}

	public void addRequest(Request request)
			throws IOException, SpinnmanException, SQLException {
		/*
		 * Ensure that the transceiver for the machine exists while we're still
		 * in the current thread; the connection inside Machine inside Request
		 * is _not_ safe to hand off between threads.
		 */
		Machine m = request.change.machine;
		txrxFactory.getTransciever(m);
		synchronized (workers) {
			workers.computeIfAbsent(m, m1 -> {
				executor.execute(() -> backgroundThread(m));
				// Temporary value; will be replaced
				return currentThread();
			});
		}
		requests.addLast(request);
		synchronized (this) {
			if (!requestsPending) {
				requestsPending = true;
			}
			notifyAll();
		}
	}

	/**
	 * The background thread for interacting with the BMP.
	 *
	 * @param machine
	 *            What SpiNNaker machine is this thread servicing?
	 */
	void backgroundThread(Machine machine) {
		synchronized (workers) {
			workers.put(machine, currentThread());
		}
		MDC.put("machine", machine.getName());
		try {
			if (onThreadStart != null) {
				onThreadStart.run();
			}

			while (true) {
				synchronized (this) {
					while (!requestsPending) {
						wait();
					}
				}

				if (!requests.isEmpty()) {
					processRequest(requests.removeFirst());
				}

				/*
				 * If nothing left in the queues, clear the request flag and
				 * break out of queue-processing loop.
				 */
				synchronized (this) {
					if (requests.isEmpty()) {
						requestsPending = false;
						notifyAll();

						/*
						 * If we've been told to stop, actually stop the thread
						 * now
						 */
						if (stop) {
							return;
						}
					}
				}
			}
		} catch (InterruptedException e) {
			// Thread is being shut down
			synchronized (this) {
				stop = true;
			}
			log.info("worker thread '{}' was interrupted",
					currentThread().getName());
		} catch (Exception e) {
			/*
			 * If the thread crashes something has gone wrong with this program
			 * (not the machine), setting stop will cause setPower and
			 * setLinkEnable to fail, hopefully propagating news of this crash.
			 */
			synchronized (this) {
				stop = true;
			}
			log.error("unhandled exception for '{}'", currentThread().getName(),
					e);
		}
	}
}
