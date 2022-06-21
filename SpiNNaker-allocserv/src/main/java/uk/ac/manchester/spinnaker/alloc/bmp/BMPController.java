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

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.slf4j.MDC.putCloseable;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.isBusy;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.UNKNOWN;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.MDC.MDCCloseable;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.TxrxProperties;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.messages.bmp.BMPBoard;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.messages.bmp.Blacklist;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException.TransientProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.utils.DefaultMap;
import uk.ac.manchester.spinnaker.utils.Ping;

/**
 * Manages the BMPs of machines controlled by Spalloc.
 *
 * @author Donal Fellows
 */
@Service("bmpController")
@ManagedResource("Spalloc:type=BMPController,name=bmpController")
public class BMPController extends DatabaseAwareBean {
	private static final Logger log = getLogger(BMPController.class);

	private boolean stop;

	private Map<Machine, WorkerState> state = new HashMap<>();

	@Autowired
	private SpallocAPI spallocCore;

	@Autowired
	private ServiceMasterControl serviceControl;

	@Autowired
	private Epochs epochs;

	@Autowired
	private TxrxProperties props;

	@Autowired
	private PhysicalSerialMapping phySerMap;

	/**
	 * Factory for {@linkplain SpiNNakerControl controllers}. Only use via
	 * {@link #getControllers(PowerRequest) getControllers(...)}.
	 */
	@Autowired
	private ObjectProvider<SpiNNakerControl> controllerFactory;

	private final ThreadGroup group = new ThreadGroup("BMP workers");

	private Deque<Cleanup> cleanupTasks = new ConcurrentLinkedDeque<>();

	/** We have our own pool. */
	private ExecutorService executor = newCachedThreadPool(this::makeThread);

	@FunctionalInterface
	private interface Cleanup {
		boolean run(AfterSQL sql);
	}

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
	// SERVICE IMPLEMENTATION

	/**
	 * Mark all pending changes as eligible for processing. Called once on
	 * application startup when all internal queues are guaranteed to be empty.
	 */
	private void clearStuckPending() {
		int changes = execute(c -> {
			try (Update u = c.update(CLEAR_STUCK_PENDING)) {
				return u.call();
			}
		});
		if (changes > 0) {
			log.info("marking {} change sets as eligible for processing",
					changes);
		}
	}

	@PostConstruct
	private void init() {
		clearStuckPending();
		// Ought to do this, but not sure about scaling
		// establishBMPConnections();
	}

	@Scheduled(fixedDelayString = "#{txrxProperties.period}",
			initialDelayString = "#{txrxProperties.period}")
	void mainSchedule() throws IOException {
		if (serviceControl.isPaused()) {
			return;
		}

		try {
			processRequests();
		} catch (DataAccessException e) {
			if (isBusy(e)) {
				log.info("database is busy; will try power processing later");
				return;
			}
			throw e;
		} catch (InterruptedException e) {
			log.error("interrupted while spawning a worker", e);
		} catch (SpinnmanException e) {
			log.error("fatal problem talking to BMP", e);
		}
	}

	/**
	 * Used to mark whether we've just cleaned up after processing a blacklist
	 * request. If we have, there is an additional epoch to bump once the
	 * transaction completes.
	 */
	private final ThreadLocal<Boolean> doneBlacklist = new ThreadLocal<>();

	/**
	 * The core of {@link #mainSchedule()}.
	 *
	 * @deprecated Only {@code public} for testing purposes.
	 * @throws IOException
	 *             If talking to the network fails
	 * @throws SpinnmanException
	 *             If a BMP sends an error back
	 * @throws InterruptedException
	 *             If the wait for workers to spawn fails.
	 */
	@Deprecated
	public void processRequests()
			throws IOException, SpinnmanException, InterruptedException {
		doneBlacklist.set(false);
		if (execute(conn -> {
			boolean changed = false;
			for (Cleanup cleanup = cleanupTasks.poll(); nonNull(
					cleanup); cleanup = cleanupTasks.poll()) {
				try (AfterSQL sql = new AfterSQL(conn)) {
					changed |= cleanup.run(sql);
				} catch (DataAccessException e) {
					log.error("problem with database", e);
				}
			}
			return changed;
		})) {
			// If anything changed, we bump the epochs
			epochs.nextJobsEpoch();
			epochs.nextMachineEpoch();
			if (doneBlacklist.get()) {
				epochs.nextBlacklistEpoch();
			}
		}
		for (Request req : takeRequests()) {
			addRequestToBMPQueue(req);
		}
	}

	/**
	 * Gets an estimate of the number of requests pending. This may include
	 * active requests that are being processed.
	 *
	 * @return The number of requests in the database queue.
	 */
	@ManagedAttribute(
			description = "An estimate of the number of requests " + "pending.")
	public int getPendingRequestLoading() {
		try (Connection conn = getConnection();
				Query countChanges = conn.query(COUNT_PENDING_CHANGES)) {
			return conn.transaction(false, () -> countChanges.call1()
					.map(integer("c")).orElse(0));
		}
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

	/** An action that may throw any of a range of exceptions. */
	private interface ThrowingAction {
		void act() throws ProcessException, IOException, InterruptedException;
	}

	private abstract class Request {
		final Machine machine;

		private int numTries = 0;

		Request(Machine machine) {
			this.machine = requireNonNull(machine);
		}

		/**
		 * @return Whether this request may be repeated.
		 */
		boolean isRepeat() {
			return numTries < props.getPowerAttempts();
		}

		/**
		 * Basic machinery for handling exceptions that arise while performing a
		 * BMP action. Runs on a thread that may touch a BMP directly, but which
		 * may not touch the database.
		 * <p>
		 * Only subclasses should use this!
		 *
		 * @param body
		 *            What to attempt.
		 * @param onFailure
		 *            What to do on failure.
		 * @param onServiceRemove
		 *            If the exception looks serious, call this to trigger a
		 *            board being taken out of service.
		 * @return Whether to stop the retry loop.
		 * @throws InterruptedException
		 *             If interrupted.
		 */
		final boolean bmpAction(ThrowingAction body,
				Consumer<Exception> onFailure,
				Consumer<Exception> onServiceRemove)
				throws InterruptedException {
			boolean isLastTry = numTries++ >= props.getPowerAttempts();
			Exception exn;
			try {
				body.act();
				// Exit the retry loop (up the stack); the requests all worked
				return true;
			} catch (InterruptedException e) {
				/*
				 * We were interrupted! This happens when we're shutting down.
				 * Log (because we're in an inconsistent state) and rethrow so
				 * that the outside gets to clean up.
				 */
				log.error("Requests failed on BMP(s) for {} because of "
						+ "interruption", machine, e);
				onFailure.accept(e);
				currentThread().interrupt();
				throw e;
			} catch (TransientProcessException e) {
				if (!isLastTry) {
					// Log somewhat gently; we *might* be able to recover...
					log.warn("Retrying requests on BMP(s) for {} after {}: {}",
							machine, props.getProbeInterval(),
							e.getMessage());
					// Ask for a retry
					return false;
				}
				exn = e;
			} catch (ProcessException | IOException | RuntimeException e) {
				exn = e;
			}
			/*
			 * Common permanent failure handling case; arrange for taking a
			 * board out of service, mark a request as failed, and stop the
			 * retry loop.
			 */
			log.error("Requests failed on BMP(s) for {}", machine, exn);
			onServiceRemove.accept(exn);
			onFailure.accept(exn);
			return true;
		}
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
	private final class PowerRequest extends Request {
		private final Map<BMPCoords, List<Integer>> powerOnBoards;

		private final Map<BMPCoords, List<Integer>> powerOffBoards;

		private final Map<BMPCoords, List<Link>> linkRequests;

		private final Integer jobId;

		private final JobState from;

		private final JobState to;

		private final List<Integer> changeIds;

		private final Map<BMPCoords, Map<Integer, BMPBoard>> idToBoard;

		private List<String> powerOnAddresses;

		/**
		 * Create a request.
		 *
		 * @param sql
		 *            How to access the database.
		 * @param machine
		 *            What machine are the boards on? <em>Must not</em> be
		 *            {@code null}.
		 * @param powerOn
		 *            What boards (by DB ID) are to be powered on? May be
		 *            {@code null}; that's equivalent to the empty list.
		 * @param powerOff
		 *            What boards (by DB ID) are to be powered off? May be
		 *            {@code null}; that's equivalent to the empty list.
		 * @param links
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
		 * @param idToBoard
		 *            How to get the physical ID of a board from its database ID
		 */
		PowerRequest(TakeReqsSQL sql, Machine machine,
				Map<BMPCoords, List<Integer>> powerOn,
				Map<BMPCoords, List<Integer>> powerOff,
				Map<BMPCoords, List<Link>> links, Integer jobId, JobState from,
				JobState to, List<Integer> changeIds,
				Map<BMPCoords, Map<Integer, BMPBoard>> idToBoard) {
			super(machine);
			powerOnBoards = isNull(powerOn) ? emptyMap() : powerOn;
			powerOffBoards = isNull(powerOff) ? emptyMap() : powerOff;
			linkRequests = isNull(links) ? emptyMap() : links;
			this.jobId = jobId;
			this.from = from;
			this.to = to;
			this.changeIds = changeIds;
			this.idToBoard = isNull(idToBoard) ? emptyMap() : idToBoard;
			/*
			 * Map this now so we keep the DB out of the way of the BMP. This
			 * mapping is not expected to change during the request's lifetime.
			 */
			powerOnAddresses = sql.transaction(() -> powerOnBoards.values()
					.stream().flatMap(Collection::stream)
					.map(boardId -> sql.getBoardAddress.call1(boardId)
							.map(string("address")).orElse(null))
					.collect(toList()));
		}

		/**
		 * Change the power state of boards in this request.
		 *
		 * @param controllers
		 *            How to actually communicate with the machine
		 * @throws ProcessException
		 *             If the transceiver chokes
		 * @throws InterruptedException
		 *             If interrupted
		 * @throws IOException
		 *             If network I/O fails
		 */
		private void changeBoardPowerState(
				Map<BMPCoords, SpiNNakerControl> controllers)
				throws ProcessException, InterruptedException, IOException {
			for (Entry<BMPCoords, Map<Integer, BMPBoard>> bmp : idToBoard
					.entrySet()) {
				// Init the real controller
				SpiNNakerControl controller = controllers.get(bmp.getKey());
				controller.setIdToBoardMap(bmp.getValue());

				// Send any power on commands
				List<Integer> on =
						powerOnBoards.getOrDefault(bmp.getKey(), emptyList());
				if (!on.isEmpty()) {
					controller.powerOnAndCheck(on);
				}

				// Process perimeter link requests next
				for (Link linkReq : linkRequests.getOrDefault(bmp.getKey(),
						emptyList())) {
					// Set the link state, as required
					controller.setLinkOff(linkReq);
				}

				// Finally send any power off commands
				List<Integer> off =
						powerOffBoards.getOrDefault(bmp.getKey(), emptyList());
				if (!off.isEmpty()) {
					controller.powerOff(off);
				}
			}
		}

		/**
		 * Handles the database changes after a set of changes to a BMP complete
		 * successfully. We will move the job to the state it supposed to be in.
		 *
		 * @param sql
		 *            How to access the DB
		 * @return Whether the state of boards or jobs has changed.
		 */
		private boolean done(AfterSQL sql) {
			int turnedOn = powerOnBoards.values().stream()
					.flatMap(Collection::stream)
					.mapToInt(board -> sql.setBoardState(true, board)).sum();
			int jobChange = sql.setJobState(to, 0, jobId);
			int turnedOff = powerOffBoards.values().stream()
					.flatMap(Collection::stream)
					.mapToInt(board -> sql.setBoardState(false, board)).sum();
			int deallocated = 0;
			if (to == DESTROYED) {
				/*
				 * Need to mark the boards as not allocated; can't do that until
				 * they've been switched off.
				 */
				deallocated = sql.deallocateBoards(jobId);
			}
			int killed = changeIds.stream().mapToInt(sql::deleteChange).sum();
			log.debug(
					"BMP ACTION SUCCEEDED ({}:{}->{}): on:{} off:{} "
							+ "jobChangesApplied:{} boardsDeallocated:{} "
							+ "bmpTasksBackedOff:{} bmpTasksDone:{}",
					jobId, from, to, turnedOn, turnedOff, jobChange,
					deallocated, 0, killed);
			return turnedOn + turnedOff > 0 || jobChange > 0;
		}

		/**
		 * Handles the database changes after a set of changes to a BMP complete
		 * with a failure. We will roll back the job state to what it was
		 * before.
		 *
		 * @param sql
		 *            How to access the DB
		 * @return Whether the state of boards or jobs has changed.
		 */
		private boolean failed(AfterSQL sql) {
			int backedOff = changeIds.stream()
					.mapToInt(changeId -> sql.setInProgress(false, changeId))
					.sum();
			int jobChange = sql.setJobState(from, 0, jobId);
			log.debug(
					"BMP ACTION FAILED ({}:{}->{}): on:{} off:{} "
							+ "jobChangesApplied:{} boardsDeallocated:{} "
							+ "bmpTasksBackedOff:{} bmpTasksDone:{}",
					jobId, from, to, 0, 0, jobChange, 0, backedOff, 0, 0);
			return jobChange > 0;
		}

		/**
		 * Ping all the boards switched on by this job. We do the pings in
		 * parallel.
		 * <p>
		 * Note that this does <em>not</em> throw if network access fails; it
		 * just puts a message in the log. That's because the board might start
		 * working in a little while. What this <em>does</em> do is help to
		 * clear the ARP cache of its unreachability state so that any VPN
		 * between here and the client won't propagate it and cause mayhem.
		 * <p>
		 * That this hack is needed is awful.
		 */
		private void ping() {
			if (serviceControl.isUseDummyBMP()) {
				// Don't bother with pings when the dummy is enabled
				return;
			}
			if (powerOnAddresses.isEmpty()) {
				// Nothing to do
				return;
			}
			log.debug("verifying network access to {} boards for job {}",
					powerOnAddresses.size(), jobId);
			powerOnAddresses.parallelStream().forEach(address -> {
				if (Ping.ping(address) != 0) {
					log.warn(
							"ARP fault? Board with address {} might not have "
									+ "come up correctly for job {}",
							address, jobId);
				}
			});
		}

		/**
		 * Process an action to power on or off a set of boards. Runs on a
		 * thread that may touch a BMP directly, but which may not touch the
		 * database.
		 *
		 * @param controllers
		 *            How to actually reach the BMPs.
		 * @return Whether this action has "succeeded" and shouldn't be retried.
		 * @throws InterruptedException
		 *             If interrupted.
		 */
		boolean tryChangePowerState(
				Map<BMPCoords, SpiNNakerControl> controllers)
				throws InterruptedException {
			return bmpAction(() -> {
				changeBoardPowerState(controllers);
				// We want to ensure the lead board is alive
				ping();
				cleanupTasks.add(this::done);
			}, e -> {
				cleanupTasks.add(this::failed);
			}, e -> {
				/*
				 * FIXME handle what happens when the hardware is unreachable
				 *
				 * When that happens, we must tell the alloc engine to pick
				 * somewhere else, and we should mark the board as out of
				 * service too; it's never going to work so taking it out right
				 * away is the only sane plan.
				 */
			});
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("PowerRequest(for=")
					.append(machine.getName());
			sb.append(";on=").append(powerOnBoards);
			sb.append(",off=").append(powerOffBoards);
			sb.append(",links=").append(linkRequests);
			return sb.append(")").toString();
		}
	}

	/**
	 * Encapsulates several queries for {@link #takeRequests()}.
	 */
	private final class TakeReqsSQL extends AbstractSQL {
		private final Query getJobIdsWithChanges = conn
				.query(getJobsWithChanges);

		private final Query getPowerChangesToDo = conn.query(GET_CHANGES);

		private final Update setInProgress = conn.update(SET_IN_PROGRESS);

		private final Query getBoardAddress = conn.query(GET_BOARD_ADDRESS);

		private final Update setJobState = conn.update(SET_STATE_PENDING);

		private final Query getBlacklistReads = conn.query(GET_BLACKLIST_READS);

		private final Query getBlacklistWrites = conn
				.query(GET_BLACKLIST_WRITES);

		@Override
		public void close() {
			getJobIdsWithChanges.close();
			getPowerChangesToDo.close();
			setInProgress.close();
			getBoardAddress.close();
			setJobState.close();
			getBlacklistReads.close();
			getBlacklistWrites.close();
			super.close();
		}

		List<BlacklistRequest> getBlacklistReads(Machine machine) {
			return getBlacklistReads.call(machine.getId())
					.map(row -> new BlacklistRequest(machine, false, row))
					.toList();
		}

		List<BlacklistRequest> getBlacklistWrites(Machine machine) {
			return getBlacklistWrites.call(machine.getId())
					.map(row -> new BlacklistRequest(machine, false, row))
					.toList();
		}
	}

	/**
	 * A request to read or write a blacklist.
	 *
	 * @author Donal Fellows
	 */
	private final class BlacklistRequest extends Request {
		private final boolean write;

		private final int opId;

		private final int boardId;

		private final BMPCoords bmp;

		private final BMPBoard board;

		private final String bmpSerialId;

		private final Blacklist blacklist;

		private BlacklistRequest(Machine machine, boolean write, Row row) {
			super(machine);
			this.write = write;
			opId = row.getInt("op_id");
			boardId = row.getInt("board_id");
			bmp = new BMPCoords(row.getInt("cabinet"), row.getInt("frame"));
			board = new BMPBoard(row.getInt("board_num"));
			if (write) {
				blacklist = row.getSerial("data", Blacklist.class);
			} else {
				blacklist = null;
			}
			bmpSerialId = row.getString("bmp_serial_id");
		}

		/** The serial number actually read from the board. */
		private String readSerial;

		private Blacklist readBlacklist;

		/**
		 * Access the DB to store the serial number information that we
		 * retrieved. Runs on a thread that may touch the database, but may not
		 * touch any BMP.
		 *
		 * @param sql
		 *            How to access the DB
		 * @return Whether we've changed anything
		 */
		private boolean recordSerialIds(AfterSQL sql) {
			return sql.setBoardSerialIds(boardId, readSerial,
					phySerMap.getPhysicalId(readSerial)) > 0;
		}

		/**
		 * Access the DB to mark the read request as successful and store the
		 * blacklist that was read. Runs on a thread that may touch the
		 * database, but may not touch any BMP.
		 *
		 * @param sql
		 *            How to access the DB
		 * @return Whether we've changed anything
		 */
		private boolean doneRead(AfterSQL sql) {
			doneBlacklist.set(true);
			return sql.completedBlacklistRead(opId, readBlacklist) > 0;
		}

		/**
		 * Access the DB to mark the write request as successful. Runs on a
		 * thread that may touch the database, but may not touch any BMP.
		 *
		 * @param sql
		 *            How to access the DB
		 * @return Whether we've changed anything
		 */
		private boolean doneWrite(AfterSQL sql) {
			doneBlacklist.set(true);
			return sql.completedBlacklistWrite(opId) > 0;
		}

		/**
		 * Access the DB to mark the request as failed and store the exception.
		 * Runs on a thread that may touch the database, but may not touch any
		 * BMP.
		 *
		 * @param sql
		 *            How to access the DB
		 * @param exn
		 *            The exception that caused the failure.
		 * @return Whether we've changed anything
		 */
		private boolean failed(AfterSQL sql, Exception exn) {
			doneBlacklist.set(true);
			return sql.failedBlacklistOp(opId, exn) > 0;
		}

		/**
		 * Access the DB to mark a board as out of service. Runs on a thread
		 * that may touch the database, but may not touch any BMP.
		 *
		 * @param sql
		 *            How to access the DB
		 * @return Whether we've changed anything
		 */
		boolean takeOutOfService(AfterSQL sql) {
			// TODO mark board as disabled
			return false;
		}

		/**
		 * Process an action to read a blacklist. Runs on a thread that may
		 * touch a BMP directly, but which may not touch the database.
		 *
		 * @param controller
		 *            How to actually reach the BMP.
		 * @return Whether this action has "succeeded" and shouldn't be retried.
		 * @throws InterruptedException
		 *             If interrupted.
		 */
		boolean readBlacklist(SpiNNakerControl controller)
				throws InterruptedException {
			return bmpAction(() -> {
				readSerial = controller.readSerial(board);
				if (bmpSerialId != null && !bmpSerialId.equals(readSerial)) {
					/*
					 * Doesn't match; WARN but keep going; hardware may just be
					 * remapped behind our back.
					 */
					log.warn(
							"blacklist read mismatch: expected serial ID '{}' "
									+ "not equal to actual serial ID '{}'",
							bmpSerialId, readSerial);
				}
				readBlacklist = controller.readBlacklist(board);
				cleanupTasks.add(this::recordSerialIds);
				cleanupTasks.add(this::doneRead);
			}, e -> {
				cleanupTasks.add(s -> failed(s, e));
			}, e -> {
				cleanupTasks.add(this::takeOutOfService);
			});
		}

		/**
		 * Process an action to write a blacklist. Runs on a thread that may
		 * touch a BMP directly, but which may not touch the database.
		 *
		 * @param controller
		 *            How to actually reach the BMP.
		 * @return Whether this action has "succeeded" and shouldn't be retried.
		 * @throws InterruptedException
		 *             If interrupted.
		 */
		boolean writeBlacklist(SpiNNakerControl controller)
				throws InterruptedException {
			return bmpAction(() -> {
				readSerial = controller.readSerial(board);
				if (bmpSerialId != null && !bmpSerialId.equals(readSerial)) {
					// Doesn't match, so REALLY unsafe to keep going!
					throw new IllegalStateException(format(
							"aborting blacklist write: expected serial ID '%s' "
									+ "not equal to actual serial ID '%s'",
							bmpSerialId, readSerial));
				}
				controller.writeBlacklist(board, requireNonNull(blacklist));
				cleanupTasks.add(this::doneWrite);
			}, e -> {
				cleanupTasks.add(s -> failed(s, e));
			}, e -> {
				cleanupTasks.add(this::takeOutOfService);
			});
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("BlacklistRequest(for=")
					.append(machine.getName());
			sb.append(";bmp=").append(bmp);
			sb.append(",board=").append(boardId);
			sb.append(",write=").append(write);
			return sb.append(")").toString();
		}
	}

	/**
	 * Copies out the requests for board power changes, marking them so that we
	 * remember they are being worked on.
	 *
	 * @return List of requests to pass to the {@link WorkerThread}s.
	 */
	private List<Request> takeRequests() {
		List<Machine> machines =
				new ArrayList<>(spallocCore.getMachines(true).values());
		try (TakeReqsSQL sql = new TakeReqsSQL()) {
			return sql.transaction(() -> {
				List<Request> requestCollector = new ArrayList<>();
				// The outer loop is always over a small set, fortunately
				for (Machine machine : machines) {
					sql.getJobIdsWithChanges.call(machine.getId())
							.map(integer("job_id"))
							.forEach(jobId -> takeRequestsForJob(machine, jobId,
									sql, requestCollector));
					requestCollector.addAll(sql.getBlacklistReads(machine));
					requestCollector.addAll(sql.getBlacklistWrites(machine));
				}
				return requestCollector;
			});
		}
	}

	private void takeRequestsForJob(Machine machine, Integer jobId,
			TakeReqsSQL sql, List<Request> requestCollector) {
		List<Integer> changeIds = new ArrayList<>();
		Map<BMPCoords, List<Integer>> boardsOn =
				new DefaultMap<>(ArrayList::new);
		Map<BMPCoords, List<Integer>> boardsOff =
				new DefaultMap<>(ArrayList::new);
		Map<BMPCoords, List<Link>> linksOff = new DefaultMap<>(ArrayList::new);
		JobState from = UNKNOWN, to = UNKNOWN;
		Map<BMPCoords, Map<Integer, BMPBoard>> idToBoard =
				new DefaultMap<>(HashMap::new);

		for (Row row : sql.getPowerChangesToDo.call(jobId)) {
			changeIds.add(row.getInteger("change_id"));
			BMPCoords bmp =
					new BMPCoords(row.getInt("cabinet"), row.getInt("frame"));
			Integer board = row.getInteger("board_id");
			idToBoard.get(bmp).put(board,
					new BMPBoard(row.getInteger("board_num")));
			boolean switchOn = row.getBoolean("power");
			/*
			 * Set these multiple times; we don't care as they should be the
			 * same for each board.
			 */
			from = row.getEnum("from_state", JobState.class);
			to = row.getEnum("to_state", JobState.class);
			if (switchOn) {
				boardsOn.get(bmp).add(board);
				/*
				 * Decode a collection of boolean columns to say which links to
				 * switch back off
				 */
				asList(Direction.values()).stream()
						.filter(link -> !row.getBoolean(link.columnName))
						.forEach(link -> linksOff.get(bmp)
								.add(new Link(board, link)));
			} else {
				boardsOff.get(bmp).add(board);
			}
		}

		if (boardsOn.isEmpty() && boardsOff.isEmpty()) {
			// Nothing to do? Oh well, though this shouldn't be reachable...
			if (to != UNKNOWN) {
				// Nothing to do, but we know the target state; just move to it
				sql.setJobState.call(to, 0, jobId);
			} else {
				// Eeep! This should be a logic bug
				log.warn("refusing to switch job {} to {} state", jobId, to);
			}
			return;
		}

		requestCollector.add(new PowerRequest(sql, machine, boardsOn, boardsOff,
				linksOff, jobId, from, to, changeIds, idToBoard));
		for (Integer changeId : changeIds) {
			sql.setInProgress.call(true, changeId);
		}
	}

	/**
	 * The profile of {@linkplain Update updates} for
	 * {@code processAfterChange()}.
	 */
	private final class AfterSQL extends AbstractSQL {
		private final Update setBoardState;

		private final Update setJobState;

		private final Update setInProgress;

		private final Update deallocateBoards;

		private final Update deleteChange;

		private final Update completedBlacklistRead;

		private final Update completedBlacklistWrite;

		private final Update failedBlacklistOp;

		private final Update setBoardSerialIds;

		AfterSQL(Connection conn) {
			super(conn);
			setBoardState = conn.update(SET_BOARD_POWER);
			setJobState = conn.update(SET_STATE_PENDING);
			setInProgress = conn.update(SET_IN_PROGRESS);
			deallocateBoards = conn.update(DEALLOCATE_BOARDS_JOB);
			deleteChange = conn.update(FINISHED_PENDING);
			completedBlacklistRead = conn.update(COMPLETED_BLACKLIST_READ);
			completedBlacklistWrite = conn.update(COMPLETED_BLACKLIST_WRITE);
			failedBlacklistOp = conn.update(FAILED_BLACKLIST_OP);
			setBoardSerialIds = conn.update(SET_BOARD_SERIAL_IDS);
		}

		@Override
		public void close() {
			setBoardSerialIds.close();
			failedBlacklistOp.close();
			completedBlacklistWrite.close();
			completedBlacklistRead.close();
			deleteChange.close();
			deallocateBoards.close();
			setInProgress.close();
			setJobState.close();
			setBoardState.close();
			super.close();
		}

		// What follows are type-safe wrappers

		int setBoardState(boolean state, Integer boardId) {
			return setBoardState.call(state, boardId);
		}

		int setJobState(JobState state, int pending, Integer jobId) {
			return setJobState.call(state, pending, jobId);
		}

		int setInProgress(boolean progress, Integer changeId) {
			return setInProgress.call(progress, changeId);
		}

		int deallocateBoards(Integer jobId) {
			return deallocateBoards.call(jobId);
		}

		int deleteChange(Integer changeId) {
			return deleteChange.call(changeId);
		}

		int completedBlacklistRead(Integer opId, Blacklist blacklist) {
			return completedBlacklistRead.call(blacklist, opId);
		}

		int completedBlacklistWrite(Integer opId) {
			return completedBlacklistWrite.call(opId);
		}

		int failedBlacklistOp(Integer opId, Exception failure) {
			return failedBlacklistOp.call(failure, opId);
		}

		int setBoardSerialIds(Integer boardId, String bmpSerialId,
				String physicalSerialId) {
			return setBoardSerialIds.call(boardId, bmpSerialId,
					physicalSerialId);
		}
	}

	private void addRequestToBMPQueue(Request request)
			throws IOException, SpinnmanException, InterruptedException {
		requireNonNull(request, "request must not be null");
		/*
		 * Ensure that the transceiver for the machine exists while we're still
		 * in the current thread; the connection inside Machine inside Request
		 * is _not_ safe to hand off between threads. Fortunately, the worker
		 * doesn't need that... provided we get the transceiver now.
		 */
		getControllers(request);
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
			WorkerState ws = state.computeIfAbsent(machine, WorkerState::new);
			ws.launchThreadIfNecessary();
			return ws;
		}
	}

	// ----------------------------------------------------------------
	// WORKER IMPLEMENTATION

	/** The state of worker threads that can be seen outside the thread. */
	private final class WorkerState {
		/** What machine is the worker handling? */
		private final Machine machine;

		/** Queue of requests to the machine to carry out. */
		private final Queue<Request> requests = new ConcurrentLinkedDeque<>();

		/**
		 * Whether there are any requests pending. Protected by a lock on the
		 * {@link BMPController} object.
		 */
		private boolean requestsPending = false;

		/**
		 * What thread is serving as the worker? Protected by a lock on the
		 * {@link BMPController#state} object.
		 */
		private Thread workerThread;

		WorkerState(Machine machine) {
			this.machine = machine;
		}

		void interrupt() {
			Thread wt = workerThread;
			if (nonNull(wt)) {
				wt.interrupt();
			}
		}

		void launchThreadIfNecessary() throws InterruptedException {
			if (isNull(workerThread)) {
				executor.execute(this::backgroundThread);
				while (isNull(workerThread)) {
					state.wait();
				}
			}
		}

		private AutoCloseable bind(Thread t) {
			t.setName("bmp-worker:" + machine.getName());
			synchronized (state) {
				workerThread = t;
				state.notifyAll();
			}
			MDCCloseable mdc = putCloseable("machine", machine.getName());
			return () -> {
				synchronized (state) {
					workerThread = null;
				}
				t.setName("bmp-worker:[unbound]");
				mdc.close();
			};
		}

		/**
		 * The background thread for interacting with the BMP.
		 */
		void backgroundThread() {
			Thread t = currentThread();

			try (AutoCloseable binding = bind(t)) {
				do {
					waitForPending(this);

					/*
					 * No lock needed; this is the only thread that removes from
					 * this queue.
					 */
					Request r = requests.poll();
					if (r != null) {
						if (r instanceof PowerRequest) {
							processRequest((PowerRequest) r);
						} else {
							processRequest((BlacklistRequest) r);
						}
					}

					/*
					 * If nothing left in the queues, clear the request flag and
					 * break out of queue-processing loop.
					 */
				} while (!shouldTerminate(this));
			} catch (InterruptedException e) {
				// Thread is being shut down
				markAllForStop();
				log.info("worker thread '{}' was interrupted", t.getName());
			} catch (Exception e) {
				/*
				 * If the thread crashes something has gone wrong with this
				 * program (not the machine), setting stop will cause setPower
				 * and setLinkEnable to fail, hopefully propagating news of this
				 * crash.
				 */
				markAllForStop();
				log.error("unhandled exception for '{}'", t.getName(), e);
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
		executor.awaitTermination(props.getProbeInterval().toMillis(),
				MILLISECONDS);
		group.interrupt();
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

	private void processRequest(BlacklistRequest request)
			throws InterruptedException {
		SpiNNakerControl controller;
		try {
			controller = getControllers(request).get(request.bmp);
		} catch (IOException | SpinnmanException e) {
			// Shouldn't ever happen; the transceiver ought to be pre-built
			log.error("could not get transceiver", e);
			return;
		}
		try (MDCCloseable mdc = putCloseable("changes",
				asList("blacklist", request.write).toString())) {
			while (request.isRepeat()) {
				if (request.write) {
					if (request.writeBlacklist(controller)) {
						break;
					}
				} else {
					if (request.readBlacklist(controller)) {
						break;
					}
				}
				sleep(props.getProbeInterval().toMillis());
			}
		}
	}

	private void processRequest(PowerRequest request)
			throws InterruptedException {
		Map<BMPCoords, SpiNNakerControl> controllers;
		try {
			controllers = getControllers(request);
		} catch (IOException | SpinnmanException e) {
			// Shouldn't ever happen; the transceiver ought to be pre-built
			log.error("could not get transceiver", e);
			return;
		}

		try (MDCCloseable mdc = putCloseable("changes",
				asList("power", request.powerOnBoards.size(),
						request.powerOffBoards.size(),
						request.linkRequests.size()).toString())) {
			while (request.isRepeat()) {
				if (request.tryChangePowerState(controllers)) {
					break;
				}
				sleep(props.getProbeInterval().toMillis());
			}
		}
	}

	/**
	 * Get the controllers for each real frame root BMP in the given request.
	 *
	 * @param request
	 *            The request, containing all the BMP coordinates.
	 * @return Map from BMP cooordinates to how to control it. These controllers
	 *         can be safely communicated with in parallel.
	 * @throws IOException
	 *             If a BMP controller fails to initialise due to network
	 *             problems.
	 * @throws SpinnmanException
	 *             If a BMP controller fails to initialise due to the BMP not
	 *             liking a message.
	 */
	private Map<BMPCoords, SpiNNakerControl> getControllers(Request request)
			throws IOException, SpinnmanException {
		try {
			if (request instanceof PowerRequest) {
				return getControllersForPower((PowerRequest) request);
			} else {
				return getControllersForBlacklisting(
						(BlacklistRequest) request);
			}
		} catch (BeanInitializationException | BeanCreationException e) {
			// Smuggle the exception out from the @PostConstruct method
			Throwable cause = e.getCause();
			if (cause instanceof IOException) {
				throw (IOException) cause;
			} else if (cause instanceof SpinnmanException) {
				throw (SpinnmanException) cause;
			}
			throw e;
		}
	}

	/**
	 * Get the controllers for each real frame root BMP in the given request.
	 *
	 * @param request
	 *            The request, containing all the BMP coordinates.
	 * @return Map from BMP cooordinates to how to control it. These controllers
	 *         can be safely communicated with in parallel.
	 */
	private Map<BMPCoords, SpiNNakerControl> getControllersForPower(
			PowerRequest request) {
		Map<BMPCoords, SpiNNakerControl> map = new HashMap<>(
				request.idToBoard.size());
		for (BMPCoords bmp : request.idToBoard.keySet()) {
			map.put(bmp, controllerFactory.getObject(request.machine, bmp));
		}
		return map;
	}

	/**
	 * Get the controller for the real frame root BMP in the given request.
	 * Blacklist management requests only ever deal with a single BMP.
	 *
	 * @param request
	 *            The request, containing the BMP coordinates.
	 * @return Map from BMP cooordinates to how to control it. These controllers
	 *         can be safely communicated with in parallel.
	 */
	private Map<BMPCoords, SpiNNakerControl> getControllersForBlacklisting(
			BlacklistRequest request) {
		Map<BMPCoords, SpiNNakerControl> map = new HashMap<>(1);
		map.put(request.bmp,
				controllerFactory.getObject(request.machine, request.bmp));
		return map;
	}

	@SuppressWarnings("unused")
	private abstract static class Use {
		Use(ThreadFactory q1, UncaughtExceptionHandler q2) {
		}
	}
}
