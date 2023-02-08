/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.model.CPUState.RUNNING;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus;
import uk.ac.manchester.spinnaker.messages.model.RouterTimeout;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * A context class that can disable dropping of packets on the SpiNNaker on-chip
 * network. <em>Use very carefully indeed!</em> A network that can't drop
 * packets is a network that can deadlock. It is not believed safe to use this
 * class on more than one board at a time, and it is definitely not safe to do
 * anything else than data transfers while the context is set.
 *
 * @author Donal Fellows
 * @author Alan Stokes
 */
public final class NoDropPacketContext implements AutoCloseable {
	private static final Logger log = getLogger(NoDropPacketContext.class);

	private ReinjectionStatus lastStatus;

	private final CoreSubsets monitorCores;

	private final TransceiverInterface txrx;

	private final ChipLocation firstChip;

	private final CoreSubsets gatherers;

	/**
	 * Standard short timeout for emergency routing.
	 */
	private static final RouterTimeout SHORT_TIMEOUT = new RouterTimeout(1, 1);

	private static final RouterTimeout LONG_TIMEOUT = new RouterTimeout(14, 14);

	private static final RouterTimeout TEMP_TIMEOUT = new RouterTimeout(15, 4);

	private static final RouterTimeout ZERO_TIMEOUT = new RouterTimeout(0, 0);

	/**
	 * Create a no-drop-packets context. This can manage multiple boards at
	 * once, but it is <em>recommended</em> that only a single board be handled
	 * by a context.
	 *
	 * @param txrx
	 *            The transceiver to use for talking to SpiNNaker.
	 * @param monitorCores
	 *            The extra monitor cores on the SpiNNaker system that control
	 *            the routers. These must be on the same board as one of the
	 *            gatherers; this is not checked.
	 * @param gatherers
	 *            The gatherer cores on the SpiNNaker system that supports the
	 *            multicast router control API.
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             If SCAMP or an extra monitor rejects a message.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@MustBeClosed
	public NoDropPacketContext(TransceiverInterface txrx,
			CoreSubsets monitorCores, CoreSubsets gatherers)
			throws IOException, ProcessException, InterruptedException {
		this.txrx = txrx;
		this.monitorCores = monitorCores;
		// Store the last reinjection status for resetting
		// NOTE: This assumes the status is the same on all cores
		var firstCore = monitorCores.first().orElseThrow();
		firstChip = firstCore.asChipLocation();
		lastStatus = txrx.getReinjectionStatus(firstCore);
		this.gatherers = gatherers;
		log.info(
				"switching board at {} ({} monitor cores) to "
						+ "non-drop mode (saved status: {})",
				firstChip, monitorCores.size(), lastStatus);
		try {
			// Set to not inject dropped packets
			txrx.setReinjection(monitorCores, false);
			// Clear any outstanding packets from reinjection
			txrx.clearReinjectionQueues(gatherers);
			// Set time outs
			txrx.setReinjectionEmergencyTimeout(gatherers, SHORT_TIMEOUT);
			txrx.setReinjectionTimeout(gatherers, LONG_TIMEOUT);
		} catch (IOException | ProcessException e) {
			log.error("failed to switch board at {} to non-drop mode",
					firstChip, e);
			throw e;
		}
	}

	/**
	 * Create a no-drop-packets context for a single board.
	 *
	 * @param txrx
	 *            The transceiver to use for talking to SpiNNaker.
	 * @param monitorCoreLocations
	 *            The extra monitor cores on the SpiNNaker system that control
	 *            the routers. These must be on the same board as the gatherer;
	 *            this is not checked.
	 * @param gatherer
	 *            The gatherer for this context and linked to these extra
	 *            monitor cores.
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             If SCAMP or an extra monitor rejects a message.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@MustBeClosed
	public NoDropPacketContext(TransceiverInterface txrx,
			CoreSubsets monitorCoreLocations, Gather gatherer)
			throws IOException, ProcessException, InterruptedException {
		this(txrx, monitorCoreLocations, convertToCoreSubset(gatherer));
	}

	/**
	 * Create a no-drop-packets context for a single board.
	 *
	 * @param txrx
	 *            The transceiver to use for talking to SpiNNaker.
	 * @param monitorCoreLocations
	 *            The extra monitor cores on the SpiNNaker system that control
	 *            the routers. These must be on the same board as the gatherer;
	 *            this is not checked.
	 * @param gatherer
	 *            The gatherer for this context and linked to these extra
	 *            monitor cores.
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             If SCAMP or an extra monitor rejects a message.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@MustBeClosed
	public NoDropPacketContext(TransceiverInterface txrx,
			List<? extends HasCoreLocation> monitorCoreLocations,
			Gather gatherer)
			throws IOException, ProcessException, InterruptedException {
		this(txrx, convertToCoreSubset(monitorCoreLocations),
				convertToCoreSubset(gatherer));
	}

	/**
	 * Create a no-drop-packets context.
	 *
	 * @param txrx
	 *            The transceiver to use for talking to SpiNNaker.
	 * @param monitorCoreLocations
	 *            The extra monitor cores on the SpiNNaker system that control
	 *            the routers. These must be on the same board as one of the
	 *            gatherers; this is not checked.
	 * @param gatherers
	 *            The gatherer for this context and linked to these extra
	 *            monitor cores.
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             If SCAMP or an extra monitor rejects a message.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@MustBeClosed
	public NoDropPacketContext(TransceiverInterface txrx,
			Stream<? extends HasCoreLocation> monitorCoreLocations,
			Stream<Gather> gatherers)
			throws IOException, ProcessException, InterruptedException {
		this(txrx, convertToCoreSubset(monitorCoreLocations),
				convertToCoreSubset(gatherers));
	}

	private static CoreSubsets convertToCoreSubset(
			List<? extends HasCoreLocation> coreLocationList) {
		var cores = new CoreSubsets();
		for (var coreLocation : coreLocationList) {
			cores.addCore(coreLocation.asCoreLocation());
		}
		return cores;
	}

	private static CoreSubsets convertToCoreSubset(Gather gather) {
		var cores = new CoreSubsets();
		cores.addCore(gather.asCoreLocation());
		return cores;
	}

	private static CoreSubsets convertToCoreSubset(
			Stream<? extends HasCoreLocation> coreLocations) {
		var cores = new CoreSubsets();
		coreLocations.forEach(loc -> cores.addCore(loc.asCoreLocation()));
		return cores;
	}

	/**
	 * Restore the SpiNNaker board (or boards, for the brave) to its normal
	 * operating mode.
	 *
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             If SCAMP or an extra monitor rejects a message.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@Override
	public void close()
			throws IOException, ProcessException, InterruptedException {
		log.info("switching board at {} to standard mode", firstChip);
		quietlySetTemporaryTimeouts();

		try {
			// Do the real reset
			txrx.setReinjectionTimeout(gatherers, lastStatus);
			txrx.setReinjectionEmergencyTimeout(gatherers, lastStatus);
			txrx.setReinjection(monitorCores, lastStatus);
			log.debug("switched board at {} to standard mode", firstChip);
			return;
		} catch (IOException | ProcessException | InterruptedException
				| RuntimeException e) {
			log.error("error resetting router timeouts", e);
			throw e;
		} catch (Exception e) {
			log.error("error resetting router timeouts", e);
		}
		try {
			log.error("checking to see of the cores are OK...");
			var errorCores = txrx.getCoresNotInState(monitorCores, RUNNING);
			if (!errorCores.isEmpty()) {
				log.error("cores in an unexpected state: {}", errorCores);
			}
		} catch (Exception e) {
			log.error("couldn't get core state", e);
		}
	}

	/**
	 * This sets some temporary timeouts so that we can use SDP more safely. We
	 * hope. Failures are ignored; if they happen, failures when setting the
	 * real values are also likely and we'll get error messages then.
	 */
	private void quietlySetTemporaryTimeouts() {
		try {
			txrx.setReinjectionTimeout(gatherers, TEMP_TIMEOUT);
		} catch (Exception e) {
			log.debug("failed to reset reinjection timeout", e);
		}
		try {
			txrx.setReinjectionEmergencyTimeout(gatherers, ZERO_TIMEOUT);
		} catch (Exception e) {
			log.debug("failed to reset emergency reinjection timeout", e);
		}
	}
}
