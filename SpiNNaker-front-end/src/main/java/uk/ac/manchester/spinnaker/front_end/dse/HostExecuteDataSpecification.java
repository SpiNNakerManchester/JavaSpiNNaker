/*
 * Copyright (c) 2018 The University of Manchester
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;
import uk.ac.manchester.spinnaker.storage.RegionInfo;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * Executes the host based data specification.
 *
 * @author Donal Fellows
 */
public class HostExecuteDataSpecification extends ExecuteDataSpecification {
	private final TransceiverInterface txrx;

	/**
	 * Create a high-level DSE interface.
	 *
	 * @param machine
	 *            The description of the SpiNNaker machine.
	 * @param db
	 *            The DSE database.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws URISyntaxException
	 *             If the URI is not valid.
	 * @throws StorageException
	 *             If the database cannot be read.
	 * @throws SpinnmanException
	 *             If there is an issue creating the transceiver.
	 * @throws IllegalStateException
	 *             If something really strange occurs with talking to the BMP;
	 *             this constructor should not be doing that!
	 */
	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	public HostExecuteDataSpecification(
			Machine machine, DSEDatabaseEngine db)
			throws IOException, InterruptedException,
			StorageException, URISyntaxException, SpinnmanException {
		super(machine, db);
		txrx = getTransceiver();
	}

	/**
	 * Execute all application data specifications that a particular connection
	 * knows about, storing back in the database the information collected about
	 * those executions.
	 *
	 * @param system
	 *            If {@code true}, loads system cores. If {@code false}, loads
	 *            non-system (i.e., application) cores.
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
	public void loadCores(boolean system)
			throws StorageException, IOException, ProcessException,
			InterruptedException {
		var storage = db.getStorageInterface();
		processTasksInParallel(storage.listEthernetsToLoad(), board -> {
			return () -> loadBoard(board, storage, system);
		});
	}

	private void loadBoard(Ethernet board, DSEStorage storage, boolean system)
			throws IOException, ProcessException, StorageException,
			InterruptedException {
		try (var c = new BoardLocal(board.location)) {
			var worker = new HostBoardWorker(txrx, board, storage);
			var regionsToWrite = new ArrayList<RegionInfo>();
			for (var xyp : storage.listCoresToLoad(board, system)) {
				worker.mallocCore(xyp);
				worker.loadCoreTable(xyp, regionsToWrite);
			}
			for (var ri : regionsToWrite) {
				worker.writeRegion(ri.core, ri.content, ri.pointer);
			}
		}
	}

	private class HostBoardWorker extends BoardWorker {
		HostBoardWorker(TransceiverInterface txrx, Ethernet board,
				DSEStorage storage) throws StorageException {
			super(txrx, board, storage);
		}

		/**
		 * Writes the contents of a region. Caller is responsible for ensuring
		 * this method has work to do.
		 *
		 * @param core
		 *            Which core to write to.
		 * @param content
		 *            Data to write
		 * @param baseAddress
		 *            Where to write the region.
		 * @return How many bytes were actually written.
		 * @throws IOException
		 *             If anything goes wrong with I/O.
		 * @throws ProcessException
		 *             If SCAMP rejects the request.
		 * @throws InterruptedException
		 *             If communications are interrupted.
		 */
		protected int writeRegion(HasCoreLocation core, ByteBuffer content,
				MemoryLocation baseAddress)
				throws IOException, ProcessException, InterruptedException {
			var data = content.duplicate();
			int written = data.remaining();
			txrx.writeMemory(core.getScampCore(), baseAddress, data);
			return written;
		}
	}

	@Override
	public void close() throws IOException {
		txrx.close();
	}
}
