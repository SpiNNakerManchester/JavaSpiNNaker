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
package uk.ac.manchester.spinnaker.front_end.dse;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/**
 * A context class that loads up the system router tables while it is active.
 *
 * @author Donal Fellows
 */
public class SystemRouterTableContext implements AutoCloseable {
	private static final Logger log = getLogger(SystemRouterTableContext.class);

	private final CoreSubsets monitorCores;

	private final Transceiver txrx;

	private final ChipLocation firstChip;

	/**
	 * Create a no-drop-packets context.
	 *
	 * @param txrx
	 *            The transceiver to use for talking to SpiNNaker.
	 * @param monitorCores
	 *            The extra monitor cores on the SpiNNaker system that control
	 *            the routers.
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             If SCAMP or an extra monitor rejects a message.
	 */
	public SystemRouterTableContext(Transceiver txrx, CoreSubsets monitorCores)
			throws IOException, ProcessException {
		this.txrx = txrx;
		this.monitorCores = monitorCores;
		CoreLocation firstCore = monitorCores.first().get();
		firstChip = firstCore.asChipLocation();

		log.info("switching multicast routing on board at {} to system mode",
				firstChip);
		log.info("will switch {} cores", monitorCores.size());
		try {
			txrx.saveApplicationRouterTables(monitorCores);
			txrx.loadSystemRouterTables(monitorCores);
		} catch (IOException | ProcessException e) {
			log.error("failed to switch multicast routing on {} to system",
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
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             If SCAMP or an extra monitor rejects a message.
	 */
	public SystemRouterTableContext(Transceiver txrx,
			List<? extends HasCoreLocation> monitorCoreLocations)
			throws IOException, ProcessException {
		this(txrx, convertToCoreSubset(monitorCoreLocations));
	}

	/**
	 * Create a no-drop-packets context.
	 *
	 * @param txrx
	 *            The transceiver to use for talking to SpiNNaker.
	 * @param monitorCoreLocations
	 *            The extra monitor cores on the SpiNNaker system that control
	 *            the routers.
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             If SCAMP or an extra monitor rejects a message.
	 */
	public SystemRouterTableContext(Transceiver txrx,
			Stream<? extends HasCoreLocation> monitorCoreLocations)
			throws IOException, ProcessException {
		this(txrx, convertToCoreSubset(monitorCoreLocations));
	}

	private static CoreSubsets convertToCoreSubset(
			List<? extends HasCoreLocation> coreLocationList) {
		CoreSubsets cores = new CoreSubsets();
		for (HasCoreLocation coreLocation : coreLocationList) {
			cores.addCore(coreLocation.asCoreLocation());
		}
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
		log.info("switching multicast routing on board at {} to standard mode",
				firstChip);

		try {
			txrx.loadApplicationRouterTables(monitorCores);
		} catch (IOException | ProcessException e) {
			log.error("error restoring multicast router tables", e);
			throw e;
		}
	}
}
