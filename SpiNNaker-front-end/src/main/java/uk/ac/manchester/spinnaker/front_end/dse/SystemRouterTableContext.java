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
package uk.ac.manchester.spinnaker.front_end.dse;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * A context class that loads up the system router tables while it is active.
 *
 * @author Donal Fellows
 */
public class SystemRouterTableContext implements AutoCloseable {
	private static final Logger log = getLogger(SystemRouterTableContext.class);

	private final CoreSubsets monitorCores;

	private final TransceiverInterface txrx;

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
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@MustBeClosed
	public SystemRouterTableContext(TransceiverInterface txrx,
			CoreSubsets monitorCores)
			throws IOException, ProcessException, InterruptedException {
		this.txrx = txrx;
		this.monitorCores = monitorCores;
		var firstCore = monitorCores.first().orElseThrow();
		firstChip = firstCore.asChipLocation();

		log.info("switching multicast routing on board at {} to system mode",
				firstChip);
		log.info("will switch {} cores", monitorCores.size());
		try {
			txrx.saveApplicationRouterTables(monitorCores);
			txrx.loadSystemRouterTables(monitorCores);
		} catch (IOException | ProcessException | InterruptedException e) {
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
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@MustBeClosed
	public SystemRouterTableContext(TransceiverInterface txrx,
			List<? extends HasCoreLocation> monitorCoreLocations)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@MustBeClosed
	public SystemRouterTableContext(TransceiverInterface txrx,
			Stream<? extends HasCoreLocation> monitorCoreLocations)
			throws IOException, ProcessException, InterruptedException {
		this(txrx, convertToCoreSubset(monitorCoreLocations));
	}

	private static CoreSubsets convertToCoreSubset(
			List<? extends HasCoreLocation> coreLocationList) {
		var cores = new CoreSubsets();
		for (var coreLocation : coreLocationList) {
			cores.addCore(coreLocation.asCoreLocation());
		}
		return cores;
	}

	private static CoreSubsets convertToCoreSubset(
			Stream<? extends HasCoreLocation> coreLocations) {
		var cores = new CoreSubsets();
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
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@Override
	public void close()
			throws IOException, ProcessException, InterruptedException {
		log.info("switching multicast routing on board at {} to standard mode",
				firstChip);

		try {
			txrx.loadApplicationRouterTables(monitorCores);
		} catch (IOException | ProcessException | InterruptedException e) {
			log.error("error restoring multicast router tables", e);
			throw e;
		}
	}
}
