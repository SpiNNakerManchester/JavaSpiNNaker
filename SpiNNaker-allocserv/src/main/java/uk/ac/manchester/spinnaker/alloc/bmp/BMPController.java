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
package uk.ac.manchester.spinnaker.alloc.bmp;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.bmp.NonBootOperation.GET_SERIAL;
import static uk.ac.manchester.spinnaker.alloc.bmp.NonBootOperation.READ_BL;
import static uk.ac.manchester.spinnaker.alloc.bmp.NonBootOperation.READ_TEMP;
import static uk.ac.manchester.spinnaker.alloc.bmp.NonBootOperation.WRITE_BL;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.google.errorprone.annotations.RestrictedApi;
import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.alloc.ForTestingOnly;
import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AllocatorProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.TxrxProperties;
import uk.ac.manchester.spinnaker.alloc.admin.ReportMailSender;
import uk.ac.manchester.spinnaker.alloc.allocator.AllocatorTask;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.machine.board.HasBMPLocation;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException.CallerProcessException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException.PermanentProcessException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException.TransientProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
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

	@Autowired
	private AllocatorTask allocator;

	private Object guard = new Object();

	@GuardedBy("guard")
	private ThreadPoolTaskScheduler scheduler;

	@GuardedBy("guard")
	private boolean emergencyStop = false;

	/**
	 * Synchronizer for power request access to the database (as otherwise
	 * deadlocks can occur when multiple transactions try to update the boards
	 * table).
	 */
	private Object powerDBSync = new Object();

	/**
	 * Map from BMP ID to worker task that handles it.
	 */
	private final Map<Integer, Worker> workers = new HashMap<>();

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

	@GuardedBy("this")
	private Throwable bmpProcessingException;

	private boolean useDummyComms = false;

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

	@PostConstruct
	private void init() {
		useDummyComms = serviceControl.isUseDummyBMP();
		synchronized (guard) {
			// Set up scheduler
			scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadGroupName("BMP");

			controllerFactory = controllerFactoryBean::getObject;
			allocator.setBMPController(this);

			// Set the pool size to match the number of workers
			makeWorkers();
			if (workers.size() > 1) {
				scheduler.setPoolSize(workers.size());
			}

			// Launch the scheduler now it is all set up
			scheduler.initialize();

			// And now use the scheduler
			for (var worker : workers.values()) {
				scheduler.scheduleAtFixedRate(worker,
						allocProps.getPeriod());
			}
		}
	}

	private List<Worker> makeWorkers() {
		// Make workers
		try (var c = getConnection();
				var getBmps = c.query(GET_ALL_BMPS);
				var getBoards = c.query(GET_ALL_BMP_BOARDS)) {
			return c.transaction(false, () -> getBmps.call(row -> {
				var m = spallocCore.getMachine(row.getString("machine_name"),
						true);
				var coords = new BMPCoords(row.getInt("cabinet"),
						row.getInt("frame"));
				var boards = new HashMap<BMPBoard, String>();
				var bmpId = row.getInt("bmp_id");
				getBoards.call(r -> {
					boards.put(new BMPBoard(r.getInt("board_num")),
							r.getString("address"));
					return null;
				}, bmpId);
				var worker = new Worker(m.get(), coords, boards, bmpId);
				workers.put(row.getInt("bmp_id"), worker);
				return worker;
			}));
		}
	}

	/**
	 * Trigger the execution of the workers for the given BMPs now.
	 *
	 * @param bmps
	 *            A list of BMPs that have changed.
	 */
	public void triggerSearch(Collection<Integer> bmps) {
		synchronized (guard) {
			if (emergencyStop) {
				log.warn("Emergency stop; not triggering workers");
				return;
			}
			for (var b : bmps) {
				var worker = workers.get(b);
				if (worker != null) {
					scheduler.schedule(() -> worker.run(), Instant.now());
				} else {
					log.error("Could not find worker for BMP {}", b);
				}
			}
		}
	}

	/**
	 * Stops execution immediately.
	 */
	public void emergencyStop() {
		synchronized (guard) {
			emergencyStop = true;
			scheduler.shutdown();
			for (var worker : workers.values()) {
				try {
					worker.getControl().powerOff(worker.boards.keySet());
				} catch (Throwable e) {
					log.warn("Error when stopping", e);
				}
			}
			execute(conn -> {
				try (var setAllOff = conn.update(SET_ALL_BOARDS_OFF)) {
					setAllOff.call();
				}
				return null;
			});
		}
	}

	/** An action that may throw any of a range of exceptions. */
	private interface ThrowingAction {
		void act() throws ProcessException, IOException, InterruptedException;
	}

	private abstract class Request {
		final int bmpId;

		private int numTries = 0;

		Request(int bmpId) {
			this.bmpId = bmpId;
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
				log.error("Requests failed on BMP {} because of "
						+ "interruption", bmpId, e);
				currentThread().interrupt();
				throw e;
			} catch (TransientProcessException e) {
				if (!isLastTry) {
					// Log somewhat gently; we *might* be able to recover...
					log.warn("Retrying requests on BMP {} after {}: {}",
							bmpId, props.getProbeInterval(),
							e.getMessage());
					// Ask for a retry
					return false;
				}
				exn = e;
				log.error("Requests failed on BMP {}", bmpId, e);
			} catch (PermanentProcessException e) {
				log.error("BMP {} on {} is unreachable", e.source, bmpId, e);
				onServiceRemove.accept(e);
				exn = e;
			} catch (CallerProcessException e) {
				// This is probably a software bug
				log.error("SW bug talking to BMP {}", bmpId, e);
				exn = e;
			} catch (ProcessException | IOException | RuntimeException e) {
				log.error("Requests failed on BMP {}", bmpId, e);
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
		final void addBoardReport(Connection c, int boardId, Integer jobId,
				String msg) {
			try (var getUser = c.query(GET_USER_DETAILS_BY_NAME);
					var insertBoardReport = c.update(INSERT_BOARD_REPORT)) {
				getUser.call1(row -> row.getInt("user_id"),
						allocProps.getSystemReportUser()).ifPresent(
								userId -> insertBoardReport.call(
										boardId, jobId,	msg, userId));
			}
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
		final void markBoardAsDead(Connection c, int boardId, String msg) {
			try (var setFunctioning = c.update(SET_FUNCTIONING_FIELD);
					var findBoardById = c.query(FIND_BOARD_BY_ID)) {
				boolean result = setFunctioning.call(false, boardId) > 0;
				if (result) {
					findBoardById.call1(row -> {
						var ser = row.getString("physical_serial_id");
						if (ser == null) {
							ser = "<UNKNOWN>";
						}
						var fullMessage = format(
								"Marked board at %d,%d,%d of %s (serial: %s) "
										+ "as dead: %s",
								row.getInt("x"), row.getInt("y"),
								row.getInt("z"), row.getString("machine_name"),
								ser, msg);
						emailSender.sendServiceMail(fullMessage);
						return null;
					}, boardId);
				}
			}
		}

		boolean processRequest(SpiNNakerControl control) {
			while (isRepeat()) {
				try {
					if (tryProcessRequest(control)) {
						return true;
					}
					sleep(props.getProbeInterval().toMillis());
				} catch (InterruptedException e) {
					// If this happens, just cancel the transaction;
					// when we come back, all things will be redone.
					throw new RuntimeException(e);
				}
			}
			return false;
		}

		abstract boolean tryProcessRequest(SpiNNakerControl control)
				throws InterruptedException;
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
		private final List<BMPBoard> powerOnBoards = new ArrayList<>();

		private final List<BMPBoard> powerOffBoards = new ArrayList<>();

		private final List<Link> linkRequests = new ArrayList<>();

		private final int jobId;

		private final JobState from;

		private final JobState to;

		private final List<Integer> changeIds = new ArrayList<>();

		private final Map<Integer, Integer> boardToId = new HashMap<>();

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
		PowerRequest(int bmpId, int jobId, JobState from, JobState to,
				List<PowerChange> powerChanges) {
			super(bmpId);
			for (var change : powerChanges) {
				if (change.power) {
					powerOnBoards.add(new BMPBoard(change.boardNum));
				} else {
					powerOffBoards.add(new BMPBoard(change.boardNum));
				}
				change.offLinks.stream().forEach(link ->
						linkRequests.add(new Link(change.boardNum, link)));
				changeIds.add(change.changeId);
				boardToId.put(change.boardNum, change.boardId);
			}
			this.jobId = jobId;
			this.from = from;
			this.to = to;
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
		void changeBoardPowerState(SpiNNakerControl controller)
				throws ProcessException, InterruptedException, IOException {

			// Send any power on commands
			if (!powerOnBoards.isEmpty()) {
				controller.powerOnAndCheck(powerOnBoards);
			}

			// Process perimeter link requests next
			for (var linkReq : linkRequests) {
				// Set the link state, as required
				controller.setLinkOff(linkReq);
			}

			// Finally send any power off commands
			if (!powerOffBoards.isEmpty()) {
				controller.powerOff(powerOffBoards);
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
		private void done() {
			try (var c = getConnection();
					var deallocateBoards = c.update(DEALLOCATE_BMP_BOARDS_JOB);
					var deleteChange = c.update(FINISHED_PENDING);
					var setBoardPowerOn = c.update(SET_BOARD_POWER_ON);
					var setBoardPowerOff = c.update(SET_BOARD_POWER_OFF)) {
				c.transaction(() -> {
					int turnedOn = powerOnBoards.stream().map(this::getBoardId)
							.mapToInt(setBoardPowerOn::call).sum();
					int turnedOff =
							powerOffBoards.stream().map(this::getBoardId)
									.mapToInt(setBoardPowerOff::call).sum();

					if (to == DESTROYED || to == QUEUED) {
						/*
						 * Need to mark the boards as not allocated; can't do
						 * that until they've been switched off.
						 */
						deallocateBoards.call(jobId, bmpId);
					}
					int completed = changeIds.stream().mapToInt(
							deleteChange::call).sum();

					log.debug("BMP ACTION SUCCEEDED ({}:{}->{}): on:{} off:{} "
							+ "completed: {}",
							jobId, from, to, turnedOn, turnedOff, completed);
				});
			}

			// Tell the allocator something has happened
			allocator.updateJob(jobId, from, to);
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
		private void failed() {
			try (var c = getConnection();
					var deallocateBoards = c.update(DEALLOCATE_BMP_BOARDS_JOB);
					var deleteChange = c.update(FINISHED_PENDING);
					var errorChange = c.update(ERROR_PENDING);
					var setBoardPowerOff = c.update(SET_BOARD_POWER_OFF)) {
				c.transaction(() -> {
					// We should mark the boards as off
					int turnedOff =
							powerOffBoards.stream().map(this::getBoardId)
									.mapToInt(setBoardPowerOff::call).sum();

					// ... even those that we should be powering on ...
					turnedOff +=
							powerOnBoards.stream().map(this::getBoardId)
									.mapToInt(setBoardPowerOff::call).sum();

					// If we are going to queued or destroyed, we can just
					// ignore the error as we will reallocate anyway
					int completed = 0;
					if (to == DESTROYED || to == QUEUED) {
						// Need to mark the boards as not allocated; slightly
						// dodgy since they might still be on, but not a lot
						// we can do about it!
						deallocateBoards.call(jobId, bmpId);
						completed = changeIds.stream().mapToInt(
								deleteChange::call).sum();
					} else {

						// If we are going to READY, we must mark changes as
						// failed to make sure we don't think we are done!
						completed = changeIds.stream().mapToInt(
								errorChange::call).sum();
					}

					log.debug(
							"BMP ACTION FAILED on {} ({}:{}->{}) off:{} "
							+ " completed {}",
							bmpId, jobId, from, to, turnedOff, completed);
				});
			}
			// Tell the allocator something has happened
			allocator.updateJob(jobId, from, to);
		}

		/**
		 * Process an action to power on or off a set of boards. Runs on a
		 * thread that may touch a BMP directly, but which may not touch the
		 * database.
		 *
		 * @param controller
		 *            How to actually reach the BMPs.
		 * @return Whether this action has "succeeded" and shouldn't be retried.
		 * @throws InterruptedException
		 *             If interrupted.
		 */
		@Override
		boolean tryProcessRequest(SpiNNakerControl controller)
				throws InterruptedException {
			boolean ok = bmpAction(() -> {
				changeBoardPowerState(controller);
				// We want to ensure the lead board is alive
				controller.ping(powerOnBoards);
				synchronized (powerDBSync) {
					done();
				}
			}, e -> {
				synchronized (powerDBSync) {
					failed();
				}
				synchronized (BMPController.this) {
					bmpProcessingException = e;
				}
			}, ppe -> {
				synchronized (powerDBSync) {
					badBoard(ppe);
				}
			});
			return ok;
		}

		@Override
		public String toString() {
			var sb = new StringBuilder("PowerRequest(for=")
					.append(bmpId);
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
		 * @param failure
		 *            The failure message.
		 * @return Whether the state of boards or jobs has changed.
		 */
		private void badBoard(ProcessException failure) {
			try (var c = getConnection()) {
				c.transaction(() -> {
					getBoardId(failure.source).ifPresent(boardId -> {
						// Mark the board as dead right now
						markBoardAsDead(c, boardId, REPORT_MSG + failure);
						// Add a report if we can
						addBoardReport(c, boardId, jobId, REPORT_MSG + failure);
					});
				});
			}
		}

		/**
		 * Given a board address, get the ID that it corresponds to. Reverses
		 * {@link #idToBoard}.
		 *
		 * @param addr
		 *            The board address.
		 * @return The ID, if one can be found.
		 */
		private Optional<Integer> getBoardId(HasBMPLocation addr) {
			return Optional.ofNullable(boardToId.get(addr.getBoard()));
		}

		private Integer getBoardId(BMPBoard board) {
			return boardToId.get(board.board);
		}
	}

	/**
	 * A request to read or write information on a BMP. Includes blacklists,
	 * serial numbers, temperature data, etc.
	 *
	 * @author Donal Fellows
	 */
	private final class BoardRequest extends Request {
		private final NonBootOperation op;

		private final int opId;

		private final int boardId;

		private final BMPCoords bmp;

		private final BMPBoard board;

		private final String bmpSerialId;

		private final Blacklist blacklist;

		private final int machineId;

		private BoardRequest(int bmpId, NonBootOperation op, Row row) {
			super(bmpId);
			this.op = op;
			opId = row.getInt("op_id");
			boardId = row.getInt("board_id");
			bmp = new BMPCoords(row.getInt("cabinet"), row.getInt("frame"));
			board = new BMPBoard(row.getInt("board_num"));
			if (op == WRITE_BL) {
				blacklist = row.getSerial("data", Blacklist.class);
			} else {
				blacklist = null;
			}
			bmpSerialId = row.getString("bmp_serial_id");
			machineId = row.getInt("machine_id");
		}

		/** The serial number actually read from the board. */
		private String readSerial;

		/**
		 * Access the DB to store the serial number information that we
		 * retrieved. A transaction should already be held.
		 *
		 * @param c
		 *            How to access the DB
		 * @return Whether we've changed anything
		 */
		private void recordSerialIds(Connection c) {
			try (var setBoardSerialIds = c.update(SET_BOARD_SERIAL_IDS)) {
				setBoardSerialIds.call(boardId, readSerial,
						phySerMap.getPhysicalId(readSerial));
			}
		}

		/**
		 * Access the DB to mark the read request as successful and store the
		 * blacklist that was read. A transaction should already be held.
		 *
		 * @param c
		 *            How to access the DB
		 * @param readBlacklist
		 *            The blacklist that was read
		 * @return Whether we've changed anything
		 */
		private void doneReadBlacklist(Connection c, Blacklist readBlacklist) {
			try (var completed = c.update(COMPLETED_BOARD_INFO_READ)) {
				log.debug("Completing blacklist read opId {}", opId);
				completed.call(readBlacklist, opId);
			}
		}

		/**
		 * Access the DB to mark the write request as successful. A transaction
		 * should already be held.
		 *
		 * @param c
		 *            How to access the DB
		 * @return Whether we've changed anything
		 */
		private void doneWriteBlacklist(Connection c) {
			try (var completed = c.update(COMPLETED_BLACKLIST_WRITE)) {
				completed.call(opId);
			}
		}

		/**
		 * Access the DB to mark the read request as successful; the actual
		 * store of the serial data is elsewhere
		 * ({@link #recordSerialIds(Connection)}). A transaction should already
		 * be held.
		 *
		 * @param c
		 *            How to access the DB
		 * @return Whether we've changed anything
		 */
		private void doneReadSerial(Connection c) {
			try (var completed = c.update(COMPLETED_GET_SERIAL_REQ)) {
				completed.call(opId);
			}
		}

		/**
		 * Access the DB to mark the read request as successful and store the
		 * ADC info that was read. A transaction should be held.
		 *
		 * @param c
		 *            The database connection.
		 */
		private void doneReadTemps(Connection c, ADCInfo adcInfo) {
			try (var completed = c.update(COMPLETED_BOARD_INFO_READ)) {
				log.debug("Completing temperature read opId {}", opId);
				completed.call(adcInfo, opId);
			}
		}

		/**
		 * Access the DB to mark the request as failed and store the exception.
		 *
		 * @param exn
		 *            The exception that caused the failure.
		 * @return Whether we've changed anything
		 */
		private void failed(Exception exn) {
			try (var c = getConnection();
					var failed = c.update(FAILED_BLACKLIST_OP)) {
				c.transaction(() -> failed.call(exn, opId));
			}
		}

		private static final String REPORT_MSG =
				"board was not reachable when trying to access its blacklist: ";

		/**
		 * Access the DB to mark a board as out of service.
		 *
		 * @param exn
		 *            The exception that caused the failure.
		 * @return Whether we've changed anything
		 */
		void takeOutOfService(Exception exn) {
			try (var c = getConnection()) {
				c.transaction(() -> {
					addBoardReport(c, boardId, null, REPORT_MSG + exn);
					markBoardAsDead(c, boardId, REPORT_MSG + exn);
				});
			}
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
		@Override
		boolean tryProcessRequest(SpiNNakerControl controller)
				throws InterruptedException {
			return bmpAction(() -> {
				switch (op) {
				case WRITE_BL:
					writeBlacklist(controller);
					break;
				case READ_BL:
					readBlacklist(controller);
					break;
				case GET_SERIAL:
					readSerial(controller);
					break;
				case READ_TEMP:
					readTemps(controller);
					break;
				default:
					throw new IllegalArgumentException();
				}
				epochs.blacklistChanged(boardId);
				epochs.machineChanged(machineId);
			}, e -> {
				failed(e);
				epochs.blacklistChanged(boardId);
				epochs.machineChanged(machineId);
			}, ppe -> {
				takeOutOfService(ppe);
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
			var readBlacklist = controller.readBlacklist(board);
			try (var c = getConnection()) {
				c.transaction(() -> {
					recordSerialIds(c);
					doneReadBlacklist(c, readBlacklist);
				});
			}
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
			try (var c = getConnection()) {
				c.transaction(() -> doneWriteBlacklist(c));
			}
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
			try (var c = getConnection()) {
				c.transaction(() -> {
					recordSerialIds(c);
					doneReadSerial(c);
				});
			}
		}

		/**
		 * Process an action to read some temperature data.
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
		private void readTemps(SpiNNakerControl controller)
				throws InterruptedException, ProcessException, IOException {
			var adcInfo = controller.readTemp(board);
			try (var c = getConnection()) {
				c.transaction(() -> doneReadTemps(c, adcInfo));
			}
		}

		@Override
		public String toString() {
			var sb = new StringBuilder("BoardRequest(for ");
			sb.append("bmp=").append(bmp);
			sb.append(",board=").append(boardId);
			sb.append(",op=").append(op);
			return sb.append(")").toString();
		}
	}

	private class PowerChange {
		final Integer changeId;

		final int jobId;

		final Integer boardId;

		final Integer boardNum;

		final Instant powerOffTime;

		final boolean power;

		final JobState from;

		final JobState to;

		final List<Direction> offLinks;

		PowerChange(Row row) {
			changeId = row.getInteger("change_id");
			jobId = row.getInt("job_id");
			boardId = row.getInteger("board_id");
			boardNum = row.getInteger("board_num");
			power = row.getBoolean("power");
			from = row.getEnum("from_state", JobState.class);
			to = row.getEnum("to_state", JobState.class);
			offLinks = List.of(Direction.values()).stream().filter(
					link -> !row.getBoolean(link.columnName)).collect(
							Collectors.toList());
			Instant powerOff = row.getInstant("power_off_timestamp");
			if (powerOff == null) {
				powerOff = Instant.EPOCH;
			}
			powerOffTime = powerOff;
		}

		boolean isSameJob(PowerChange p) {
			return p.jobId == jobId && p.from == from && p.to == to;
		}
	}

	// ----------------------------------------------------------------
	// WORKER IMPLEMENTATION

	/** A worker of a given BMP. */
	private final class Worker implements Runnable {
		/** What are we controlling? */
		private SpiNNakerControl control;

		private final SpallocAPI.Machine machine;

		private final BMPCoords coords;

		private final Map<BMPBoard, String> boards;

		/** Which boards are we looking at? */
		private final int bmpId;

		Worker(SpallocAPI.Machine machine, BMPCoords coords,
				Map<BMPBoard, String> boards, int bmpId) {
			this.machine = machine;
			this.coords = coords;
			this.boards = boards;
			this.bmpId = bmpId;

			log.debug("Created worker for boards {}", bmpId);
		}

		private SpiNNakerControl getControl() {
			if (control == null) {
				if (useDummyComms) {
					control = new SpiNNakerControlDummy();
				} else {
					try {
						control = controllerFactory.create(machine, coords,
								boards);
					} catch (Exception e) {
						log.error("Could not create control for BMP '{}'",
								bmpId, e);
					}
				}
			}
			return control;
		}

		/**
		 * Periodically call to update, or trigger externally.
		 */
		@Override
		public synchronized void run() {
			log.trace("Searching for changes on BMP {}", bmpId);

			try {
				var changes = getRequestedOperations();
				for (var change : changes) {
					change.processRequest(getControl());
				}
			} catch (Exception e) {
				log.error("unhandled exception for BMP '{}'", bmpId, e);
			}
		}

		private boolean waitedLongEnough(PowerChange change) {
			// Power off can be done any time
			if (!change.power) {
				return true;
			}

			// Power on should wait until a time after last off
			Instant powerOnTime = change.powerOffTime.plus(
					props.getOffWaitTime());
			return powerOnTime.isBefore(Instant.now());
		}

		/**
		 * Get the things that we want the worker to do. <em>Be very
		 * careful!</em> Because this necessarily involves the database, this
		 * must not touch the BMP handle as those operations take a long time
		 * and we absolutely must not have a transaction open at the same time.
		 *
		 * @return List of operations to perform.
		 */
		private List<Request> getRequestedOperations() {
			var requests = new ArrayList<Request>();
			try (var c = getConnection();
					var getPowerRequests = c.query(GET_CHANGES);
					var getBlacklistReads = c.query(GET_BLACKLIST_READS);
					var getBlacklistWrites = c.query(GET_BLACKLIST_WRITES);
					var getReadSerialInfos = c.query(GET_SERIAL_INFO_REQS);
					var getReadTemps = c.query(GET_TEMP_INFO_REQS)) {
				c.transaction(false, () -> {
					// Batch power requests by job
					var powerChanges = new LinkedList<>(
							getPowerRequests.call(PowerChange::new, bmpId));
					while (!powerChanges.isEmpty()) {
						var change = powerChanges.poll();
						var jobChanges = new ArrayList<>(List.of(change));
						var canDoNow = waitedLongEnough(change);
						while (!powerChanges.isEmpty()
								&& change.isSameJob(powerChanges.peek())) {
							canDoNow &= waitedLongEnough(powerChanges.peek());
							jobChanges.add(powerChanges.poll());
						}
						if (!jobChanges.isEmpty() && canDoNow) {
							log.debug("Running job changes {}", jobChanges);
							requests.add(new PowerRequest(bmpId, change.jobId,
									change.from, change.to, jobChanges));
						}
					}

					// Leave these until quiet
					if (requests.isEmpty()) {
						requests.addAll(getBlacklistReads.call(
								row -> new BoardRequest(bmpId, READ_BL, row),
								bmpId));
					}
					if (requests.isEmpty()) {
						requests.addAll(getBlacklistWrites.call(
								row -> new BoardRequest(bmpId, WRITE_BL, row),
								bmpId));
						requests.addAll(getReadSerialInfos.call(
								row -> new BoardRequest(bmpId, GET_SERIAL, row),
								bmpId));
						requests.addAll(getReadTemps.call(
								row -> new BoardRequest(bmpId, READ_TEMP, row),
								bmpId));
					}
				});
			} catch (Exception e) {
				log.error("unhandled exception for BMP '{}'", bmpId, e);
			}
			return requests;
		}
	}

	/**
	 * The testing interface.
	 *
	 * @hidden
	 */
	@ForTestingOnly
	public interface TestAPI {
		/**
		 * Ensure things are set up after a database change that updates the
		 * BMPs in the system.
		 *
		 * @param useDummyComms Whether to use dummy communications in the test
		 */
		void prepare(boolean useDummyComms);

		/**
		 * Reset the transceivers stored in the workers after installing a new
		 * transceiver.
		 */
		void resetTransceivers();

		/**
		 * The core of the scheduler.
		 *
		 * @param millis
		 *            How many milliseconds to sleep before doing a rerun of the
		 *            scheduler. If zero (or less), only one run will be done.
		 * @param bmps
		 *            The BMPs to be updated.
		 * @throws IOException
		 *             If talking to the network fails
		 * @throws SpinnmanException
		 *             If a BMP sends an error back
		 * @throws InterruptedException
		 *             If the wait for workers to spawn fails.
		 */
		void processRequests(long millis, Collection<Integer> bmps)
				throws IOException, SpinnmanException, InterruptedException;

		/**
		 * The core of the scheduler. Will process for all known BMPs.
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

		/**
		 * Get the last BMP exception.
		 *
		 * @return The exception.
		 */
		Throwable getBmpException();

		/**
		 * Clear the last BMP exception.
		 */
		void clearBmpException();

		/**
		 * Resume after emergency stop.
		 */
		void emergencyResume();
	}

	/**
	 * @return The test interface.
	 * @deprecated This interface is just for testing.
	 * @hidden
	 */
	@ForTestingOnly
	@RestrictedApi(explanation = "just for testing", link = "index.html",
			allowedOnPath = ".*/src/test/java/.*")
	@Deprecated
	public final TestAPI getTestAPI() {
		ForTestingOnly.Utils.checkForTestClassOnStack();
		return new TestAPI() {
			@Override
			public void prepare(boolean useDummyCommsParam) {
				useDummyComms = useDummyCommsParam;
				makeWorkers();
			}

			@Override
			public void resetTransceivers() {
				for (var worker : workers.values()) {
					worker.control = null;
				}
			}

			@Override
			public void processRequests(long millis, Collection<Integer> bmps)
					throws IOException, SpinnmanException,
					InterruptedException {
				/*
				 * Runs twice because it takes two cycles to fully process a
				 * request.
				 */
				triggerSearch(bmps);
				if (millis > 0) {
					Thread.sleep(millis);
					triggerSearch(bmps);
				}
			}

			@Override
			public void processRequests(long millis) throws IOException,
					SpinnmanException, InterruptedException {
				processRequests(millis, workers.keySet());
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

			@Override
			public void emergencyResume() {
				synchronized (guard) {
					emergencyStop = false;
					workers.clear();
				}
				init();
			}
		};
	}
}
