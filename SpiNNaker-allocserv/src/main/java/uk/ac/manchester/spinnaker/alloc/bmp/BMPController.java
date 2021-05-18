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
import static java.util.Collections.unmodifiableCollection;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_OFF;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_ON;
import static uk.ac.manchester.spinnaker.utils.InetFactory.getByName;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.annotation.PreDestroy;

import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.allocator.Machine;
import uk.ac.manchester.spinnaker.alloc.allocator.PowerState;
import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

@Component
public class BMPController {
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

	private Map<Machine, Transceiver> txrxMap = new HashMap<>();

	private boolean requestsPending;

	private volatile boolean stop;

	private ConcurrentLinkedDeque<Request> requests;

	private Runnable onThreadStart;

	private Map<Machine, Thread> workers = new HashMap<>();

	@PreDestroy
	private void shutDownWorkers() {
		stop = true;
		synchronized (this) {
			notifyAll();
		}
		for (Thread worker : workers.values()) {
			worker.interrupt();
		}
	}

	private Transceiver txrx(Machine machine)
			throws IOException, SpinnmanException, SQLException {
		synchronized (txrxMap) {
			Transceiver t = txrxMap.get(machine);
			if (t == null) {
				InetAddress address =
						getByName(machine.getRootBoardBMPAddress());
				List<Integer> boards = machine.getBoardNumbers();
				BMPConnectionData c = new BMPConnectionData(0, 0, address,
						boards, SCP_SCAMP_PORT);
				t = new Transceiver(null, asList(new BMPConnection(c)), null,
						null, null, null, null);
				txrxMap.put(machine, t);
			}
			return t;
		}
	}

	private boolean isGoodFPGA(Machine machine, Transceiver txrx, int board,
			int fpga) throws ProcessException, IOException {
		int fpgaId = txrx.readFPGARegister(fpga, FPGA_FLAG_REGISTER_ADDRESS, 0,
				0, board);
		boolean ok = (fpgaId & FPGA_FLAG_ID_MASK) == fpga;
		if (!ok) {
			log.warn("FPGA {} on board {} of {} has incorrect FPGA ID flag {}",
					fpga, board, machine.name, fpgaId & FPGA_FLAG_ID_MASK);
		}
		return ok;
	}

	private void setLinkState(Transceiver txrx, int board, LinkInfo link,
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
	 * The boards must be on a single machine.
	 *
	 * @author Donal Fellows
	 */
	public static class Request {
		private final Machine machine;

		private final List<Integer> powerOnBoards;

		private final List<Integer> powerOffBoards;

		private final List<LinkRequest> linkRequests;

		private final OnDone onDone;

		/**
		 * Create a request.
		 *
		 * @param machine
		 *            What machine are the boards on?
		 * @param powerOnBoards
		 *            What boards (by physical ID) are to be powered on?
		 * @param powerOffBoards
		 *            What boards (by physical ID) are to be powered off?
		 * @param linkRequests
		 *            Any link power control requests. By default, links are on
		 *            if their board is on and they are connected; it is
		 *            <em>useful and relevant</em> to modify the power state of
		 *            links on the periphery of an allocation.
		 * @param onDone
		 *            An optional callback for when the changes are fully
		 *            processed (whether successfully or not). May be
		 *            {@code null} if there is no callback.
		 */
		public Request(Machine machine, List<Integer> powerOnBoards,
				List<Integer> powerOffBoards, List<LinkRequest> linkRequests,
				OnDone onDone) {
			this.machine = machine;
			this.powerOnBoards = new ArrayList<>(powerOnBoards);
			this.powerOffBoards = new ArrayList<>(powerOffBoards);
			this.linkRequests = new ArrayList<>(linkRequests);
			this.onDone = onDone;
		}

		/**
		 * Create a request.
		 *
		 * @param machine
		 *            What machine are the boards on?
		 * @param powerOnBoards
		 *            What boards (by physical ID) are to be powered on?
		 * @param powerOffBoards
		 *            What boards (by physical ID) are to be powered off?
		 * @param linkRequests
		 *            Any link power control requests. By default, links are on
		 *            if their board is on and they are connected; it is
		 *            <em>useful and relevant</em> to modify the power state of
		 *            links on the periphery of an allocation.
		 */
		public Request(Machine machine, List<Integer> powerOnBoards,
				List<Integer> powerOffBoards, List<LinkRequest> linkRequests) {
			this.machine = machine;
			this.powerOnBoards = new ArrayList<>(powerOnBoards);
			this.powerOffBoards = new ArrayList<>(powerOffBoards);
			this.linkRequests = new ArrayList<>(linkRequests);
			this.onDone = null;
		}

		/**
		 * Create a request.
		 *
		 * @param machine
		 *            What machine are the boards on?
		 * @param powerOnBoards
		 *            What boards (by physical ID) are to be powered on?
		 * @param linkRequests
		 *            Any link power control requests. By default, links are on
		 *            if their board is on and they are connected; it is
		 *            <em>useful and relevant</em> to modify the power state of
		 *            links on the periphery of an allocation.
		 * @param onDone
		 *            An optional callback for when the changes are fully
		 *            processed (whether successfully or not). May be
		 *            {@code null} if there is no callback.
		 */
		public Request(Machine machine, List<Integer> powerOnBoards,
				List<LinkRequest> linkRequests, OnDone onDone) {
			this.machine = machine;
			this.powerOnBoards = new ArrayList<>(powerOnBoards);
			this.powerOffBoards = new ArrayList<>();
			this.linkRequests = new ArrayList<>(linkRequests);
			this.onDone = onDone;
		}

		/**
		 * Create a request.
		 *
		 * @param machine
		 *            What machine are the boards on?
		 * @param powerOnBoards
		 *            What boards (by physical ID) are to be powered on?
		 * @param linkRequests
		 *            Any link power control requests. By default, links are on
		 *            if their board is on and they are connected; it is
		 *            <em>useful and relevant</em> to modify the power state of
		 *            links on the periphery of an allocation.
		 */
		public Request(Machine machine, List<Integer> powerOnBoards,
				List<LinkRequest> linkRequests) {
			this.machine = machine;
			this.powerOnBoards = new ArrayList<>(powerOnBoards);
			this.powerOffBoards = new ArrayList<>();
			this.linkRequests = new ArrayList<>(linkRequests);
			this.onDone = null;
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

		private final LinkInfo link;

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
		public LinkRequest(int board, LinkInfo link, PowerState power) {
			this.board = board;
			this.link = link;
			this.power = power;
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
			txrx = txrx(request.machine);
		} catch (IOException | SpinnmanException | SQLException e) {
			log.error("could not get transceiver", e);
			return;
		}
		MDC.put("changes", asList(request.powerOnBoards.size(),
				request.powerOffBoards.size(), request.linkRequests.size()));
		try {
			for (int nTries = 0; nTries++ < N_REQUEST_TRIES;) {
				try {
					// Send any power on commands
					if (!request.powerOnBoards.isEmpty()) {
						powerOnAndCheck(request.machine, txrx,
								request.powerOnBoards);
					}

					// Process link requests next
					for (LinkRequest linkReq : request.linkRequests) {
						// Set the link state, as required
						setLinkState(txrx, linkReq.board, linkReq.link,
								linkReq.power);
					}

					// Finally send any power off commands
					if (!request.powerOffBoards.isEmpty()) {
						txrx.power(POWER_OFF, 0, 0, request.powerOffBoards);
					}

					// Exit the retry loop if the requests all worked
					if (request.onDone != null) {
						request.onDone.call(null, null);
					}
					break;
				} catch (InterruptedException e) {
					String reason = "Requests failed on BMP " + request.machine;
					log.error(reason, e);
					if (request.onDone != null) {
						request.onDone.call(reason, e);
					}
					throw e;
				} catch (Exception e) {
					if (nTries == N_REQUEST_TRIES) {
						String reason =
								"Requests failed on BMP " + request.machine;
						log.error(reason, e);
						if (request.onDone != null) {
							request.onDone.call(reason, e);
						}
						currentThread().interrupt();
						break;
					}
					log.error(
							"Retrying requests on BMP {} after {} seconds: {}",
							request.machine, SECONDS_BETWEEN_TRIES,
							e.getMessage());
					sleep(SECONDS_BETWEEN_TRIES * MS_PER_S);
				}
			}
		} finally {
			MDC.remove("changes");
		}
	}

	public void addRequest(Request request)
			throws IOException, SpinnmanException, SQLException {
		/*
		 * Ensure that the transceiver for the machine exists while we're still
		 * in the current thread; the connection inside Machine inside Request
		 * is _not_ safe to hand off between threads.
		 */
		txrx(request.machine);
		synchronized (workers) {
			workers.computeIfAbsent(request.machine, m -> {
				Thread t = new Thread(() -> backgroundThread(m),
						"BMP worker for " + m);
				t.start();
				return t;
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
		MDC.put("machine", machine.name);
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

	public enum LinkInfo {
		/** Link to board to East. (x+1,y) */
		EAST(0, 0),
		/** Link to board to South. (x,y-1) */
		SOUTH(0, 1),
		/** Link to board to South West. (x-1,y-1) */
		SOUTH_WEST(1, 0),
		/** Link to board to West. (x-1,y) */
		WEST(1, 1),
		/** Link to board to North. (x,y+1) */
		NORTH(2, 0),
		/** Link to board to North East. (x+1,y-1) */
		NORTH_EAST(2, 1);

		private static final int REG_STOP_OFFSET = 0x5C;

		private static final int OFFSET_FACTOR = 0x00010000;

		private final int fpga;

		private final int addr;

		LinkInfo(int fpga, int offset) {
			this.fpga = fpga;
			this.addr = offset * OFFSET_FACTOR + REG_STOP_OFFSET;
		}
	}
}
