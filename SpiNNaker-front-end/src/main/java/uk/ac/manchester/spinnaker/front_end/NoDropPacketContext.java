/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.model.CPUState.RUNNING;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.CPUInfo;
import uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus;
import uk.ac.manchester.spinnaker.messages.model.RouterTimeout;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

/**
 * A context class that can disable dropping of packets on the SpiNNaker on-chip
 * network. <em>Use very carefully indeed!</em> A network that can't drop
 * packets is a network that can deadlock. It is not safe to use this class on
 * more than one board at a time or while anything other than data transfer is
 * being done.
 *
 * @author Donal Fellows
 * @author Alan Stokes
 */
public class NoDropPacketContext implements AutoCloseable {
	private static final Logger log = getLogger(NoDropPacketContext.class);
	private ReinjectionStatus lastStatus;
	private final CoreSubsets monitorCores;
	private final Transceiver txrx;
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
	 * Create a no-drop-packets context.
	 *
	 * @param txrx
	 *            The transceiver to use for talking to SpiNNaker.
	 * @param monitorCores
	 *            The extra monitor cores on the SpiNNaker system that control
	 *            the routers.
	 * @param gatherers
	 *            The gatherer core on the SpiNNaker system that supports the
	 *            multicast router control API.
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             If SCAMP or an extra monitor rejects a message.
	 */
	public NoDropPacketContext(Transceiver txrx, CoreSubsets monitorCores,
			CoreSubsets gatherers) throws IOException, ProcessException {
		this.txrx = txrx;
		this.monitorCores = monitorCores;
		// Store the last reinjection status for resetting
		// NOTE: This assumes the status is the same on all cores
		CoreLocation firstCore = monitorCores.iterator().next();
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
	 * Create a no-drop-packets context.
	 *
	 * @param txrx
	 *            The transceiver to use for talking to SpiNNaker.
	 * @param monitorCoreLocations
	 *            The extra monitor cores on the SpiNNaker system that control
	 *            the routers.
	 * @param gatherer
	 *            The gatherer for this context and linked to these extra
	 *            monitor cores.
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             If SCAMP or an extra monitor rejects a message.
	 */
	public NoDropPacketContext(Transceiver txrx,
			CoreSubsets monitorCoreLocations, Gather gatherer)
			throws IOException, ProcessException {
		this(txrx, monitorCoreLocations, convertToCoreSubset(gatherer));
	}

	/**
	 * Create a no-drop-packets context.
	 *
	 * @param txrx
	 *            The transceiver to use for talking to SpiNNaker.
	 * @param monitorCoreLocations
	 *            The extra monitor cores on the SpiNNaker system that control
	 *            the routers.
	 * @param gatherer
	 *            The gatherer for this context and linked to these extra
	 *            monitor cores.
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             If SCAMP or an extra monitor rejects a message.
	 */
	public NoDropPacketContext(Transceiver txrx,
			List<? extends HasCoreLocation> monitorCoreLocations, Gather gatherer)
			throws IOException, ProcessException {
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
	 *            the routers.
	 * @param gatherers
	 *            The gatherer for this context and linked to these extra
	 *            monitor cores.
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             If SCAMP or an extra monitor rejects a message.
	 */
	public NoDropPacketContext(Transceiver txrx,
			Stream<? extends HasCoreLocation> monitorCoreLocations,
			Stream<Gather> gatherers) throws IOException, ProcessException {
		this(txrx, convertToCoreSubset(monitorCoreLocations),
				convertToCoreSubset(gatherers));
	}

	private static CoreSubsets convertToCoreSubset(
			List<? extends HasCoreLocation> coreLocationList) {
		CoreSubsets cores = new CoreSubsets();
		for (HasCoreLocation coreLocation : coreLocationList) {
			cores.addCore(coreLocation.asCoreLocation());
		}
		return cores;
	}

	private static CoreSubsets convertToCoreSubset(Gather gather) {
		CoreSubsets cores = new CoreSubsets();
		cores.addCore(gather.asCoreLocation());
		return cores;
	}

	private static CoreSubsets convertToCoreSubset(
			Stream<? extends HasCoreLocation> coreLocations) {
		CoreSubsets cores = new CoreSubsets();
		coreLocations.forEach(loc -> cores.addCore(loc.asCoreLocation()));
		return cores;
	}

	/**
	 * Restore the SpiNNaker board to its normal operating mode.
	 *
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             If SCAMP or an extra monitor rejects a message.
	 */
	@Override
	public void close() throws IOException, ProcessException {
		log.info("switching board at {} to standard mode", firstChip);
		quietlySetTemporaryTimeouts();

		try {
			// Do the real reset
			txrx.setReinjectionTimeout(gatherers, lastStatus);
			txrx.setReinjectionEmergencyTimeout(gatherers, lastStatus);
			txrx.setReinjection(monitorCores, lastStatus);
			log.debug("switched board at {} to standard mode", firstChip);
			return;
		} catch (Exception e) {
			log.error("error resetting router timeouts", e);
		}
		try {
			log.error("checking to see of the cores are OK...");
			Map<CoreLocation, CPUInfo> errorCores =
					txrx.getCoresNotInState(monitorCores, RUNNING);
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
		} catch (Exception ignored) {
		}
		try {
			txrx.setReinjectionEmergencyTimeout(gatherers, ZERO_TIMEOUT);
		} catch (Exception ignored) {
		}
	}
}
