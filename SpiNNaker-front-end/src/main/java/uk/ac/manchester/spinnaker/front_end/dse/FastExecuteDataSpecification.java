/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.dse;

import static java.lang.System.getProperty;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.front_end.download.request.Monitor;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.protocols.NoDropPacketContext;
import uk.ac.manchester.spinnaker.protocols.SystemRouterTableContext;
import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * Implementation of the Data Specification Executor that uses the Fast Data In
 * protocol to upload the results to a SpiNNaker machine.
 *
 * @author Donal Fellows
 * @author Alan Stokes
 */
public class FastExecuteDataSpecification extends ExecuteDataSpecification {
	private static final Logger log =
			getLogger(FastExecuteDataSpecification.class);

	private static final String SPINNAKER_COMPARE_UPLOAD =
			getProperty("spinnaker.compare.upload");

	private final Map<ChipLocation, Gather> gathererForChip;

	private final Map<ChipLocation, Monitor> monitorForChip;

	private final Map<ChipLocation, CoreSubsets> monitorsForBoard;

	/**
	 * Create an instance of this class.
	 *
	 * @param txrx
	 *            The transceiver for talking to the SpiNNaker machine.
	 * @param machine
	 *            The SpiNNaker machine description.
	 * @param gatherers
	 *            The description of where the gatherers and monitors are.
	 * @param reportDir
	 *            Where to write reports, or {@code null} if no reports are to
	 *            be written.
	 * @param db
	 *            The DSE Database.
	 * @throws IOException
	 *             If IO goes wrong.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws URISyntaxException
	 *             If the proxy URI is provided but not valid.
	 * @throws StorageException
	 *             If there is an error reading the database.
	 * @throws IllegalStateException
	 *             If something really strange occurs with talking to the BMP;
	 *             this constructor should not be doing that!
	 */
	@MustBeClosed
	public FastExecuteDataSpecification(TransceiverInterface txrx,
			Machine machine, List<Gather> gatherers, File reportDir,
			DSEDatabaseEngine db) throws IOException, ProcessException,
			InterruptedException, StorageException, URISyntaxException {
		super(txrx, machine, db);
		if (SPINNAKER_COMPARE_UPLOAD != null) {
			log.warn(
					"detailed comparison of uploaded data enabled; "
					+ "this may destabilize the protocol");
		}

		gathererForChip = new HashMap<>();
		monitorForChip = new HashMap<>();
		monitorsForBoard = new HashMap<>();

		buildMaps(gatherers);
	}

	/**
	 * Construct the internal mappings for gatherers and monitors.
	 *
	 * @param gatherers
	 *            The descriptions of whether the gatherers are located.
	 * @throws IOException
	 *             If IDs can't be read from the machine for network reasons.
	 * @throws ProcessException
	 *             If IDs can't be read from the machine for machine reasons.
	 * @throws InterruptedException
	 *             If we are interrupted.
	 */
	protected void buildMaps(List<Gather> gatherers)
			throws IOException, ProcessException, InterruptedException {
		for (var g : gatherers) {
			g.updateTransactionIdFromMachine(txrx);
			var gathererChip = g.asChipLocation();
			gathererForChip.put(gathererChip, g);
			var boardMonitorCores = monitorsForBoard
					.computeIfAbsent(gathererChip, __ -> new CoreSubsets());
			for (var m : g.getMonitors()) {
				var monitorChip = m.asChipLocation();
				gathererForChip.put(monitorChip, g);
				monitorForChip.put(monitorChip, m);
				boardMonitorCores.addCore(m.asCoreLocation());
			}
		}
	}

	/**
	 * Execute all application data specifications that a particular connection
	 * knows about, storing back in the database the information collected about
	 * those executions. Data is transferred using the Fast Data In protocol.
	 * <p>
	 * Cannot load data for system cores; those are used by the implementation
	 * of this protocol.
	 *
	 * @throws StorageException
	 *             If the database can't be talked to.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws IllegalStateException
	 *             If an unexpected exception occurs in any of the parallel
	 *             tasks.
	 */
	public void loadCores()
			throws StorageException, IOException, ProcessException,
			InterruptedException {
		var storage = db.getStorageInterface();
		processTasksInParallel(storage.listEthernetsToLoad(), board -> {
			return () -> loadBoard(board, storage);
		});
	}

	private static CoreSubsets convertToCoreSubset(Gather gather) {
		var cores = new CoreSubsets();
		cores.addCore(gather.asCoreLocation());
		return cores;
	}

	private void loadBoard(Ethernet board, DSEStorage storage)
			throws IOException, ProcessException, StorageException,
			InterruptedException {
		var cores = storage.listCoresToLoad(board, false);
		if (cores.isEmpty()) {
			log.info("no cores need loading on board; skipping");
			return;
		}
		log.info("loading data onto {} cores on board", cores.size());
		var gather = gathererForChip.get(board.location);
		var worker = new FastBoardWorker(gather, txrx, board, storage);
		for (var xyp : cores) {
			worker.mallocCore(xyp);
		}
		var monitors = monitorsForBoard.get(board.location);
		try (var routers = new SystemRouterTableContext(txrx, monitors);
				var context = new NoDropPacketContext(txrx, monitors,
						convertToCoreSubset(gather))) {
			for (var xyp : cores) {
				worker.loadCore(xyp);
			}
			log.info("finished sending data in for this board");
		} catch (Exception e) {
			log.warn("failure in core loading", e);
			throw e;
		}
	}

	/**
	 * The worker class that handles a particular board of a SpiNNaker machine.
	 * Instances of this class are only ever used from a single thread.
	 *
	 * @author Donal Fellows
	 * @author Alan Stokes
	 */
	private class FastBoardWorker extends BoardWorker {
		private final Gather gather;

		FastBoardWorker(Gather gather, TransceiverInterface txrx,
				Ethernet board, DSEStorage storage) throws StorageException {
			super(txrx, board, storage);
			this.gather = gather;
		}

		@Override
		protected int writeRegion(HasCoreLocation core, ByteBuffer content,
				MemoryLocation baseAddress)
				throws IOException, ProcessException, InterruptedException {
			int toWrite = content.remaining();
			txrx.writeMemoryFast(gather.asCoreLocation(), board.location,
					board.ethernetAddress, gather.getIptag(), core, baseAddress,
					content);
			return toWrite;
		}
	}
}
