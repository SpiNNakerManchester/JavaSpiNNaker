/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.bmp;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.slf4j.MDC.putCloseable;
import static uk.ac.manchester.spinnaker.alloc.bmp.BlacklistOperation.GET_SERIAL;
import static uk.ac.manchester.spinnaker.alloc.bmp.BlacklistOperation.READ;
import static uk.ac.manchester.spinnaker.alloc.bmp.BlacklistOperation.WRITE;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.isBusy;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.READY;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.UNKNOWN;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.curry;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.annotations.RestrictedApi;
import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.alloc.ForTestingOnly;
import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AllocatorProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.TxrxProperties;
import uk.ac.manchester.spinnaker.alloc.admin.ReportMailSender;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Update;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException.CallerProcessException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException.PermanentProcessException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException.TransientProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.utils.DefaultMap;
import uk.ac.manchester.spinnaker.utils.Ping;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

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

	private final Map<Machine, WorkerState> state = new HashMap<>();

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

	@Autowired
	private AllocatorProperties allocProps;

	@Autowired
	private ReportMailSender emailSender;

	/**
	 * Factory for {@linkplain SpiNNakerControl controllers}. Only use via
	 * {@link #controllerFactory}.
	 */
	@Autowired
	private ObjectProvider<SpiNNakerControl> controllerFactoryBean;

	/**
	 * Type-safe factory for {@linkplain SpiNNakerControl controllers}.
	 */
	private SpiNNakerControl.Factory controllerFactory;

	private final ThreadGroup group = new ThreadGroup("BMP workers");

	private Deque<Function<AfterSQL, Boolean>> cleanupTasks =
			new ConcurrentLinkedDeque<>();

	private Deque<Runnable> postCleanupTasks = new ConcurrentLinkedDeque<>();

	/** We have our own pool. */
	private ExecutorService executor = newCachedThreadPool(this::makeThread);

	@GuardedBy("this")
	private Throwable bmpProcessingException;

	/**
	 * What jobs are currently being processed? Matters because they might be
	 * busy because of BMPs that are busy with reloading FPGAs, which is a very
	 * slow business because bandwidth isn't great on the management channel.
	 */
	private final Set<Integer> busyJobs = new ConcurrentSkipListSet<>();

	/**
	 * A {@link ThreadFactory}.
	 *
	 * @param target
	 *            What the thread will be doing.
	 * @return The thread.
	 */
	@UsedInJavadocOnly(ThreadFactory.class)
	private Thread makeThread(Runnable target) {
		var t = new Thread(group, target);
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
	@UsedInJavadocOnly(UncaughtExceptionHandler.class)
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
			try (var u = c.update(CLEAR_STUCK_PENDING)) {
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
		controllerFactory = controllerFactoryBean::getObject;
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
	 * @throws IOException
	 *             If talking to the network fails
	 * @throws SpinnmanException
	 *             If a BMP sends an error back
	 * @throws InterruptedException
	 *             If the wait for workers to spawn fails.
	 */
	private void processRequests()
			throws IOException, SpinnmanException, InterruptedException {
		doneBlacklist.set(false);
		if (execute(conn -> {
			boolean changed = false;
			for (var cleanup = cleanupTasks.poll(); nonNull(cleanup);
					cleanup = cleanupTasks.poll()) {
				log.debug("processing cleanup: {}", cleanup);
				try (var sql = new AfterSQL(conn)) {
					changed |= cleanup.apply(sql);
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
		for (var postCleanup = postCleanupTasks.poll();
				nonNull(postCleanup); postCleanup = postCleanupTasks.poll()) {
			log.debug("processing postCleanup: {}", postCleanup);
			postCleanup.run();
		}
		for (var req : takeRequests()) {
			log.debug("processing request: {}", req);
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
		try (var conn = getConnection();
				var countChanges = conn.query(COUNT_PENDING_CHANGES)) {
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
				Consumer<PermanentProcessException> onServiceRemove)
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
				log.error("Requests failed on BMP(s) for {}", machine, e);
			} catch (PermanentProcessException e) {
				log.error("BMP {} on {} is unreachable", e.core, machine, e);
				onServiceRemove.accept(e);
				exn = e;
			} catch (CallerProcessException e) {
				// This is probably a software bug
				log.error("SW bug talking to BMP(s) for {}", machine, e);
				exn = e;
			} catch (ProcessException | IOException | RuntimeException e) {
				log.error("Requests failed on BMP(s) for {}", machine, e);
				exn = e;
			}
			/*
			 * Common permanent failure handling case; arrange for taking a
			 * board out of service, mark a request as failed, and stop the
			 * retry loop.
			 */
			onFailure.accept(exn);
			return true;
		}

		/**
		 * Add a report to the database of a problem with a board.
		 *
		 * @param sql
		 *            How to talk to the DB
		 * @param boardId
		 *            Which board has the problem
		 * @param jobId
		 *            What job was associated with the problem (if any)
		 * @param msg
		 *            Information about what the problem was
		 */
		final void addBoardReport(AfterSQL sql, int boardId, Integer jobId,
				String msg) {
			sql.getUser(allocProps.getSystemReportUser())
					.ifPresent(userId -> sql.insertBoardReport(boardId, jobId,
							msg, userId));
		}

		/**
		 * Marks a board as actually dead, and requests we send email about it.
		 *
		 * @param sql
		 *            How to talk to the DB
		 * @param boardId
		 *            Which board has the problem
		 * @param msg
		 *            Information about what the problem was
		 * @return Whether we've successfully done a change.
		 */
		final boolean markBoardAsDead(AfterSQL sql, int boardId, String msg) {
			boolean result = sql.markBoardAsDead(boardId) > 0;
			if (result) {
				sql.findBoardById.call1(boardId).ifPresent(row -> {
					var ser = row.getString("physical_serial_id");
					if (ser == null) {
						ser = "<UNKNOWN>";
					}
					var fullMessage = format(
							"Marked board at %d,%d,%d of %s (serial: %s) "
									+ "as dead: %s",
							row.getInt("x"), row.getInt("y"), row.getInt("z"),
							row.getString("machineName"), ser, msg);
					// Postpone email sending until out of transaction
					postCleanupTasks.add(
							() -> emailSender.sendServiceMail(fullMessage));
				});
			}
			return result;
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
			powerOnBoards = isNull(powerOn) ? Map.of() : powerOn;
			powerOffBoards = isNull(powerOff) ? Map.of() : powerOff;
			linkRequests = isNull(links) ? Map.of() : links;
			this.jobId = jobId;
			this.from = from;
			this.to = to;
			this.changeIds = changeIds;
			this.idToBoard = isNull(idToBoard) ? Map.of() : idToBoard;
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
			for (var bmp : idToBoard.entrySet()) {
				// Init the real controller
				var controller = controllers.get(bmp.getKey());
				controller.setIdToBoardMap(bmp.getValue());

				// Send any power on commands
				var on = powerOnBoards.getOrDefault(bmp.getKey(), List.of());
				if (!on.isEmpty()) {
					controller.powerOnAndCheck(on);
				}

				// Process perimeter link requests next
				for (var linkReq : linkRequests.getOrDefault(bmp.getKey(),
						List.of())) {
					// Set the link state, as required
					controller.setLinkOff(linkReq);
				}

				// Finally send any power off commands
				var off = powerOffBoards.getOrDefault(bmp.getKey(), List.of());
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
			busyJobs.remove(jobId);
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
			busyJobs.remove(jobId);
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
				synchronized (BMPController.this) {
					bmpProcessingException = e;
				}
			}, ppe -> {
				/*
				 * It's OK (not great, but OK) for things to be unreachable when
				 * the board is being turned off at the end of a job.
				 */
				if (to == READY && powerOffBoards.isEmpty()) {
					cleanupTasks.add(curry(this::badBoard, ppe));
				}
			});
		}

		@Override
		public String toString() {
			var sb = new StringBuilder("PowerRequest(for=")
					.append(machine.getName());
			sb.append(";on=").append(powerOnBoards);
			sb.append(",off=").append(powerOffBoards);
			sb.append(",links=").append(linkRequests);
			return sb.append(")").toString();
		}

		private static final String REPORT_MSG =
				"board was not reachable when trying to power it: ";

		/**
		 * When a BMP is unroutable, we must tell the alloc engine to pick
		 * somewhere else, and we should mark the board as out of service too;
		 * it's never going to work so taking it out right away is the only sane
		 * plan. We also need to nuke the planned changes. Retrying is bad.
		 *
		 * @param sql
		 *            How to access the DB.
		 * @param failure
		 *            The failure message.
		 * @return Whether the state of boards or jobs has changed.
		 */
		private boolean badBoard(ProcessException failure, AfterSQL sql) {
			boolean changed = false;
			// Mark job for reallocation
			changed |= sql.setJobState(QUEUED, 0, jobId) > 0;
			// Mark boards allocated to job as free
			changed |= sql.deallocateBoards(jobId) > 0;
			// Delete all queued BMP commands
			sql.deleteChangesForJob(jobId);
			getBoardId(failure.core).ifPresent(boardId -> {
				// Mark the board as dead right now
				markBoardAsDead(sql, boardId, REPORT_MSG + failure);
				// Add a report if we can
				addBoardReport(sql, boardId, jobId, REPORT_MSG + failure);
			});
			return changed;
		}

		/**
		 * Given a board address, get the ID that it corresponds to. Reverses
		 * {@link #idToBoard}.
		 *
		 * @param addr
		 *            The board address.
		 * @return The ID, if one can be found.
		 */
		private Optional<Integer> getBoardId(HasCoreLocation addr) {
			return idToBoard.get(new BMPCoords(addr.getX(), addr.getY()))
					.entrySet().stream()
					.filter(ib2 -> ib2.getValue().board == addr.getP())
					.map(Entry::getKey).findFirst();
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

		private final Query getSerials = conn.query(GET_SERIAL_INFO_REQS);

		@Override
		public void close() {
			getJobIdsWithChanges.close();
			getPowerChangesToDo.close();
			setInProgress.close();
			getBoardAddress.close();
			setJobState.close();
			getBlacklistReads.close();
			getBlacklistWrites.close();
			getSerials.close();
			super.close();
		}

		List<BlacklistRequest> getBlacklistReads(Machine machine) {
			return getBlacklistReads.call(machine.getId())
					.map(row -> new BlacklistRequest(machine, READ, row))
					.toList();
		}

		List<BlacklistRequest> getBlacklistWrites(Machine machine) {
			return getBlacklistWrites.call(machine.getId())
					.map(row -> new BlacklistRequest(machine, WRITE, row))
					.toList();
		}

		List<BlacklistRequest> getReadSerialInfos(Machine machine) {
			return getSerials.call(machine.getId())
					.map(row -> new BlacklistRequest(machine, GET_SERIAL, row))
					.toList();
		}
	}

	/**
	 * A request to read or write a blacklist.
	 *
	 * @author Donal Fellows
	 */
	private final class BlacklistRequest extends Request {
		private final BlacklistOperation op;

		private final int opId;

		private final int boardId;

		private final BMPCoords bmp;

		private final BMPBoard board;

		private final String bmpSerialId;

		private final Blacklist blacklist;

		private BlacklistRequest(Machine machine, BlacklistOperation op,
				Row row) {
			super(machine);
			this.op = op;
			opId = row.getInt("op_id");
			boardId = row.getInt("board_id");
			bmp = new BMPCoords(row.getInt("cabinet"), row.getInt("frame"));
			board = new BMPBoard(row.getInt("board_num"));
			if (op == WRITE) {
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
		 * Access the DB to mark the write request as successful. Runs on a
		 * thread that may touch the database, but may not touch any BMP.
		 *
		 * @param sql
		 *            How to access the DB
		 * @return Whether we've changed anything
		 */
		private boolean doneReadSerial(AfterSQL sql) {
			doneBlacklist.set(true);
			return sql.completedGetSerialReq(opId) > 0;
		}

		/**
		 * Access the DB to mark the request as failed and store the exception.
		 * Runs on a thread that may touch the database, but may not touch any
		 * BMP.
		 *
		 * @param exn
		 *            The exception that caused the failure.
		 * @param sql
		 *            How to access the DB
		 * @return Whether we've changed anything
		 */
		private boolean failed(Exception exn, AfterSQL sql) {
			doneBlacklist.set(true);
			return sql.failedBlacklistOp(opId, exn) > 0;
		}

		private static final String REPORT_MSG =
				"board was not reachable when trying to access its blacklist: ";

		/**
		 * Access the DB to mark a board as out of service. Runs on a thread
		 * that may touch the database, but may not touch any BMP.
		 *
		 * @param exn
		 *            The exception that caused the failure.
		 * @param sql
		 *            How to access the DB
		 * @return Whether we've changed anything
		 */
		boolean takeOutOfService(Exception exn, AfterSQL sql) {
			addBoardReport(sql, boardId, null, REPORT_MSG + exn);
			return markBoardAsDead(sql, boardId, REPORT_MSG + exn);
		}

		/**
		 * Process an action to work with a blacklist or serial number. Runs on
		 * a thread that may touch a BMP directly, but which may not touch the
		 * database.
		 *
		 * @param controller
		 *            How to actually reach the BMP.
		 * @return Whether this action has "succeeded" and shouldn't be retried.
		 * @throws InterruptedException
		 *             If interrupted.
		 */
		boolean perform(SpiNNakerControl controller)
				throws InterruptedException {
			return bmpAction(() -> {
				switch (op) {
				case WRITE:
					writeBlacklist(controller);
					break;
				case READ:
					readBlacklist(controller);
					break;
				case GET_SERIAL:
					readSerial(controller);
					break;
				default:
					throw new IllegalArgumentException();
				}
			}, e -> {
				cleanupTasks.add(curry(this::failed, e));
			}, ppe -> {
				cleanupTasks.add(curry(this::takeOutOfService, ppe));
			});
		}

		/**
		 * Process an action to read a blacklist.
		 *
		 * @param controller
		 *            How to actually reach the BMP.
		 * @throws InterruptedException
		 *             If interrupted.
		 * @throws IOException
		 *             If the network is unhappy.
		 * @throws ProcessException
		 *             If the BMP rejects a message.
		 */
		private void readBlacklist(SpiNNakerControl controller)
				throws InterruptedException, ProcessException, IOException {
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
		}

		/**
		 * Process an action to write a blacklist.
		 *
		 * @param controller
		 *            How to actually reach the BMP.
		 * @throws InterruptedException
		 *             If interrupted.
		 * @throws IOException
		 *             If the network is unhappy.
		 * @throws ProcessException
		 *             If the BMP rejects a message.
		 * @throws IllegalStateException
		 *             If the operation is applied to a board other than the one
		 *             that it is expected to apply to.
		 */
		private void writeBlacklist(SpiNNakerControl controller)
				throws InterruptedException, ProcessException, IOException {
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
		}

		/**
		 * Process an action to read the serial number from a BMP.
		 *
		 * @param controller
		 *            How to actually reach the BMP.
		 * @throws InterruptedException
		 *             If interrupted.
		 * @throws IOException
		 *             If the network is unhappy
		 * @throws ProcessException
		 *             If the BMP rejects a message.
		 */
		private void readSerial(SpiNNakerControl controller)
				throws InterruptedException, ProcessException, IOException {
			readSerial = controller.readSerial(board);
			cleanupTasks.add(this::recordSerialIds);
			cleanupTasks.add(this::doneReadSerial);
		}

		@Override
		public String toString() {
			var sb = new StringBuilder("BlacklistRequest(for=")
					.append(machine.getName());
			sb.append(";bmp=").append(bmp);
			sb.append(",board=").append(boardId);
			sb.append(",op=").append(op);
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
		var machines = List.copyOf(spallocCore.getMachines(true).values());
		try (var sql = new TakeReqsSQL()) {
			return sql.transaction(() -> {
				var requestCollector = new ArrayList<Request>();
				// The outer loop is always over a small set, fortunately
				for (var machine : machines) {
					sql.getJobIdsWithChanges.call(machine.getId())
							.map(integer("job_id"))
							.filter(jobId -> !busyJobs.contains(jobId))
							.forEach(jobId -> takeRequestsForJob(machine, jobId,
									sql, requestCollector));
					requestCollector.addAll(sql.getBlacklistReads(machine));
					requestCollector.addAll(sql.getBlacklistWrites(machine));
					requestCollector.addAll(sql.getReadSerialInfos(machine));
				}
				return requestCollector;
			});
		}
	}

	private void takeRequestsForJob(Machine machine, Integer jobId,
			TakeReqsSQL sql, List<Request> requestCollector) {
		var changeIds = new ArrayList<Integer>();
		var boardsOn = new DefaultMap<BMPCoords, List<Integer>>(ArrayList::new);
		var boardsOff =
				new DefaultMap<BMPCoords, List<Integer>>(ArrayList::new);
		var linksOff = new DefaultMap<BMPCoords, List<Link>>(ArrayList::new);
		JobState from = UNKNOWN, to = UNKNOWN;
		var idToBoard =
				new DefaultMap<BMPCoords, Map<Integer, BMPBoard>>(HashMap::new);
		busyJobs.add(jobId);

		for (var row : sql.getPowerChangesToDo.call(jobId)) {
			changeIds.add(row.getInteger("change_id"));
			var bmp = new BMPCoords(row.getInt("cabinet"), row.getInt("frame"));
			var board = row.getInteger("board_id");
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
				List.of(Direction.values()).stream()
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
		for (var changeId : changeIds) {
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

		private final Update completedGetSerialReq;

		private final Update failedBlacklistOp;

		private final Update setBoardSerialIds;

		private final Update deleteChangesForJob;

		private final Update insertBoardReport;

		private final Update setBoardFunctioning;

		private final Query getUser;

		private final Query findBoardById;

		AfterSQL(Connection conn) {
			super(conn);
			setBoardState = conn.update(SET_BOARD_POWER);
			setJobState = conn.update(SET_STATE_PENDING);
			setInProgress = conn.update(SET_IN_PROGRESS);
			deallocateBoards = conn.update(DEALLOCATE_BOARDS_JOB);
			deleteChange = conn.update(FINISHED_PENDING);
			completedBlacklistRead = conn.update(COMPLETED_BLACKLIST_READ);
			completedBlacklistWrite = conn.update(COMPLETED_BLACKLIST_WRITE);
			completedGetSerialReq = conn.update(COMPLETED_GET_SERIAL_REQ);
			failedBlacklistOp = conn.update(FAILED_BLACKLIST_OP);
			setBoardSerialIds = conn.update(SET_BOARD_SERIAL_IDS);
			deleteChangesForJob = conn.update(KILL_JOB_PENDING);
			insertBoardReport = conn.update(INSERT_BOARD_REPORT);
			setBoardFunctioning = conn.update(SET_FUNCTIONING_FIELD);
			getUser = conn.query(GET_USER_ID);
			findBoardById = conn.query(FIND_BOARD_BY_ID);
		}

		@Override
		public void close() {
			findBoardById.close();
			getUser.close();
			setBoardFunctioning.close();
			insertBoardReport.close();
			deleteChangesForJob.close();
			setBoardSerialIds.close();
			failedBlacklistOp.close();
			completedGetSerialReq.close();
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

		int completedGetSerialReq(Integer opId) {
			return completedGetSerialReq.call(opId);
		}

		int failedBlacklistOp(Integer opId, Exception failure) {
			return failedBlacklistOp.call(failure, opId);
		}

		int setBoardSerialIds(Integer boardId, String bmpSerialId,
				String physicalSerialId) {
			return setBoardSerialIds.call(boardId, bmpSerialId,
					physicalSerialId);
		}

		int deleteChangesForJob(Integer jobId) {
			return deleteChangesForJob.call(jobId);
		}

		int insertBoardReport(
				int boardId, Integer jobId, String issue, int userId) {
			return insertBoardReport.key(boardId, jobId, issue, userId)
					.orElseThrow();
		}

		int markBoardAsDead(Integer boardId) {
			return setBoardFunctioning.call(false, boardId);
		}

		Optional<Integer> getUser(String userName) {
			return getUser.call1(userName).map(integer("user_id"));
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
		getWorkerState(request.machine).addRequest(request);
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
			var ws = state.computeIfAbsent(machine, WorkerState::new);
			ws.launchThreadIfNecessary();
			return ws;
		}
	}

	private List<WorkerState> listWorkers() {
		synchronized (state) {
			return List.copyOf(state.values());
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
		@GuardedBy("BMPController.this")
		private boolean requestsPending = false;

		/**
		 * What thread is serving as the worker? Protected by a lock on the
		 * {@link BMPController#state} object.
		 */
		@GuardedBy("state")
		private Thread workerThread;

		WorkerState(Machine machine) {
			this.machine = machine;
		}

		void interrupt() {
			synchronized (state) {
				var wt = workerThread;
				if (nonNull(wt)) {
					wt.interrupt();
				}
			}
		}

		void launchThreadIfNecessary() throws InterruptedException {
			synchronized (state) {
				if (isNull(workerThread)) {
					executor.execute(this::backgroundThread);
					while (isNull(workerThread)) {
						state.wait();
					}
				}
			}
		}

		@MustBeClosed
		private AutoCloseable bind(Thread t) {
			t.setName("bmp-worker:" + machine.getName());
			synchronized (state) {
				workerThread = t;
				state.notifyAll();
			}
			var mdc = putCloseable("machine", machine.getName());
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
			var t = currentThread();

			try (var binding = bind(t)) {
				do {
					waitForPending();

					/*
					 * No lock needed; this is the only thread that removes from
					 * this queue.
					 */
					var r = requests.poll();
					if (r instanceof PowerRequest) {
						processRequest((PowerRequest) r);
					} else if (r instanceof BlacklistRequest) {
						processRequest((BlacklistRequest) r);
					}

					/*
					 * If nothing left in the queues, clear the request flag and
					 * break out of queue-processing loop.
					 */
				} while (!shouldTerminate());
			} catch (InterruptedException e) {
				// Thread is being shut down
				markAllForStop();
				log.debug("worker thread '{}' was interrupted", t.getName());
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

		/**
		 * Add a request to this worker's input queue.
		 *
		 * @param request
		 *            The request to add. Not {@code null}.
		 */
		void addRequest(Request request) {
			requests.add(request);
			synchronized (BMPController.this) {
				if (!requestsPending) {
					requestsPending = true;
				}
				BMPController.this.notifyAll();
			}
		}

		private void waitForPending() throws InterruptedException {
			synchronized (BMPController.this) {
				while (!requestsPending) {
					BMPController.this.wait();
				}
			}
		}

		private boolean shouldTerminate() {
			synchronized (BMPController.this) {
				if (requests.isEmpty()) {
					requestsPending = false;
					BMPController.this.notifyAll();

					if (stop) {
						return true;
					}
				}
				return false;
			}
		}
	}

	@PreDestroy
	private void shutDownWorkers() throws InterruptedException {
		markAllForStop();
		executor.shutdown();
		for (var ws : listWorkers()) {
			ws.interrupt();
		}
		executor.awaitTermination(props.getProbeInterval().toMillis(),
				MILLISECONDS);
		group.interrupt();
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
		try (var mdc = putCloseable("changes",
				List.of("blacklist", request.op).toString())) {
			while (request.isRepeat()) {
				if (request.perform(controller)) {
					return;
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

		try (var mdc = putCloseable("changes",
				List.of("power", request.powerOnBoards.size(),
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
	 * @return Map from BMP coordinates to how to control it. These controllers
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
			var cause = e.getCause();
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
	 * @return Map from BMP coordinates to how to control it. These controllers
	 *         can be safely communicated with in parallel.
	 */
	private Map<BMPCoords, SpiNNakerControl> getControllersForPower(
			PowerRequest request) {
		var map = new HashMap<BMPCoords, SpiNNakerControl>(
				request.idToBoard.size());
		for (var bmp : request.idToBoard.keySet()) {
			map.put(bmp, controllerFactory.create(request.machine, bmp));
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
		return Map.of(request.bmp,
				controllerFactory.create(request.machine, request.bmp));
	}

	/**
	 * The testing interface.
	 */
	@ForTestingOnly
	public interface TestAPI {
		/**
		 * The core of the scheduler.
		 *
		 * @param millis
		 *            How many milliseconds to sleep before doing a rerun of the
		 *            scheduler. If zero (or less), only one run will be done.
		 * @throws IOException
		 *             If talking to the network fails
		 * @throws SpinnmanException
		 *             If a BMP sends an error back
		 * @throws InterruptedException
		 *             If the wait for workers to spawn fails.
		 */
		void processRequests(long millis)
				throws IOException, SpinnmanException, InterruptedException;

		Throwable getBmpException();

		void clearBmpException();
	}

	/**
	 * @return The test interface.
	 * @deprecated This interface is just for testing.
	 */
	@ForTestingOnly
	@RestrictedApi(explanation = "just for testing", link = "index.html",
			allowedOnPath = ".*/src/test/java/.*")
	@Deprecated
	public final TestAPI getTestAPI() {
		ForTestingOnly.Utils.checkForTestClassOnStack();
		return new TestAPI() {
			@Override
			public void processRequests(long millis) throws IOException,
					SpinnmanException, InterruptedException {
				/*
				 * Runs twice because it takes two cycles to fully process a
				 * request.
				 */
				BMPController.this.processRequests();
				Thread.sleep(millis);
				BMPController.this.processRequests();
			}

			@Override
			public Throwable getBmpException() {
				synchronized (BMPController.this) {
					return bmpProcessingException;
				}
			}

			@Override
			public void clearBmpException() {
				synchronized (BMPController.this) {
					bmpProcessingException = null;
				}
			}
		};
	}
}
