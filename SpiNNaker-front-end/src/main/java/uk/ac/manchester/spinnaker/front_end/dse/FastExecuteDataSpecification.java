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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.protocols.FastDataIn;
import uk.ac.manchester.spinnaker.protocols.NoDropPacketContext;
import uk.ac.manchester.spinnaker.protocols.SystemRouterTableContext;
import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;
import uk.ac.manchester.spinnaker.storage.RegionInfo;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
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

	private final TransceiverInterface txrx;

	private final MachineDimensions machineDimensions;

	/**
	 * Create an instance of this class.
	 *
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
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws URISyntaxException
	 *             If the proxy URI is provided but not valid.
	 * @throws StorageException
	 *             If there is an error reading the database.
	 * @throws SpinnmanException
	 *            If there is an error creating the transceiver.
	 * @throws IllegalStateException
	 *             If something really strange occurs with talking to the BMP;
	 *             this constructor should not be doing that!
	 */
	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	public FastExecuteDataSpecification(
			Machine machine, List<Gather> gatherers, File reportDir,
			DSEDatabaseEngine db) throws IOException, InterruptedException,
			StorageException, URISyntaxException, SpinnmanException {
		super(machine, db);
		if (SPINNAKER_COMPARE_UPLOAD != null) {
			log.warn(
					"detailed comparison of uploaded data enabled; "
					+ "this may destabilize the protocol");
		}
		txrx = getTransceiver();
		machineDimensions = db.getStorageInterface().getMachineDimensions();
	}

	@Override
	public void close() throws IOException {
		txrx.close();
	}

	/**
	 * Execute all application data specifications that a particular connection
	 * knows about, storing back in the database the information collected about
	 * those executions. Data is transferred using the Fast Data In protocol.
	 * <p>
	 * Cannot load data for system cores; those are used by the implementation
	 * of this protocol.
	 *
	 * @param gatherers
	 *             The receivers of data from the machine.
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
	public void loadCores(List<Gather> gatherers)
			throws StorageException, IOException, ProcessException,
			InterruptedException {
		var storage = db.getStorageInterface();
		var gathersByBoard = new HashMap<String, Gather>();
		for (var gather : gatherers) {
			gathersByBoard.put(
					gather.getIptag().getBoardAddress().getHostAddress(),
					gather);
		}
		var regionsToWrite = new HashMap<Ethernet, List<RegionInfo>>();
		processTasksInParallel(storage.listEthernetsToLoad(), board -> {
			return () -> {
				var items = loadBoardTables(board, storage);
				synchronized (regionsToWrite) {
					regionsToWrite.put(board, items);
				}
			};
		});

		try (var s = new SystemRouterTableContext(txrx,
				gatherers.stream().flatMap(g -> g.getMonitors().stream()));
				var p = new NoDropPacketContext(txrx,
						gatherers.stream()
								.flatMap(g -> g.getMonitors().stream()),
						gatherers.stream())) {
			log.info("launching {} parallel high-speed upload tasks",
					regionsToWrite.size());
			processTasksInParallel(regionsToWrite.keySet(), board -> {
				return () -> {
					var gather = gathersByBoard.get(board.ethernetAddress);
					loadBoardData(gather, regionsToWrite.get(board));
				};
			});
		}
	}

	private List<RegionInfo> loadBoardTables(Ethernet board, DSEStorage storage)
			throws IOException, ProcessException, StorageException,
			InterruptedException {
		var cores = storage.listCoresToLoad(board, false);
		if (cores.isEmpty()) {
			log.info("no cores need loading on board; skipping");
			return List.of();
		}
		log.info("loading data onto {} cores on board", cores.size());
		var worker = new BoardWorker(txrx, board, storage);
		var regionsToWrite = new ArrayList<RegionInfo>();
		for (var xyp : cores) {
			worker.mallocCore(xyp);
			worker.loadCoreTable(xyp, regionsToWrite);
		}
		return regionsToWrite;
	}

	@SuppressWarnings("MustBeClosed")
	private void loadBoardData(Gather gather, List<RegionInfo> regionsToWrite)
			throws IOException, StorageException, ProcessException,
			InterruptedException {
		FastDataIn fastDataIn = null;
		var job = getJob();
		if (job != null) {
			fastDataIn = new FastDataIn(gather.asCoreLocation(),
					gather.getIptag());
		}

		for (var info : regionsToWrite) {
			var core = getBoardLocalDestination(info.core, gather);
			if (fastDataIn != null) {
				fastDataIn.fastWrite(core, info.pointer, info.content);
			} else {
				job.fastWriteData(gather.asCoreLocation(), gather.getIptag(),
						core, info.pointer, info.content);
			}
		}
		if (fastDataIn != null) {
			fastDataIn.close();
		}
	}

	private HasChipLocation getBoardLocalDestination(
			HasChipLocation monitorChip, Gather gather) {
		int boardLocalX = monitorChip.getX() - gather.getX();
		if (boardLocalX < 0) {
			boardLocalX += machineDimensions.width;
		}
		int boardLocalY = monitorChip.getY() - gather.getY();
		if (boardLocalY < 0) {
			boardLocalY += machineDimensions.height;
		}
		return new ChipLocation(boardLocalX, boardLocalY);
	}
}
