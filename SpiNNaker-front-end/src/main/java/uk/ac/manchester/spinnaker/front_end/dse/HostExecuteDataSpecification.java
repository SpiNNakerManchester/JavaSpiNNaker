/*
 * Copyright (c) 2018 The University of Manchester
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

import static java.lang.Integer.toUnsignedLong;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.front_end.Constants.CORE_DATA_SDRAM_BASE_TAG;
import static uk.ac.manchester.spinnaker.front_end.Constants.PARALLEL_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_BYTE_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.data_spec.DataSpecificationException;
import uk.ac.manchester.spinnaker.data_spec.Executor;
import uk.ac.manchester.spinnaker.data_spec.MemoryRegion;
import uk.ac.manchester.spinnaker.data_spec.MemoryRegionReal;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor.SimpleCallable;
import uk.ac.manchester.spinnaker.front_end.BoardLocalSupport;
import uk.ac.manchester.spinnaker.front_end.Progress;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.storage.ConnectionProvider;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.CoreToLoad;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/**
 * Executes the host based data specification.
 *
 * @author Donal Fellows
 */
public class HostExecuteDataSpecification extends BoardLocalSupport
		implements AutoCloseable {
	private static final String LOADING_MSG =
			"loading data specifications onto SpiNNaker";

	private static final Logger log =
			getLogger(HostExecuteDataSpecification.class);

	/**
	 * Global thread pool for DSE execution.
	 */
	private final BasicExecutor executor;

	private final Machine machine;

	private final Transceiver txrx;

	/**
	 * Create a high-level DSE interface.
	 *
	 * @param machine
	 *            The description of the SpiNNaker machine.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws IllegalStateException
	 *             If something really strange occurs with talking to the BMP;
	 *             this constructor should not be doing that!
	 */
	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	public HostExecuteDataSpecification(Machine machine)
			throws IOException, ProcessException {
		super(machine);
		executor = new BasicExecutor(PARALLEL_SIZE);
		this.machine = machine;
		try {
			txrx = new Transceiver(machine);
		} catch (ProcessException e) {
			throw e;
		} catch (SpinnmanException e) {
			throw new IllegalStateException("failed to talk to BMP, "
					+ "but that shouldn't have happened at all", e);
		}
	}

	/**
	 * Run the tasks in parallel. Submits to {@link #executor} and detoxifies
	 * the exceptions.
	 *
	 * @param tasks
	 *            The tasks to run.
	 * @throws StorageException
	 *             If the database can't be talked to.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws DataSpecificationException
	 *             If a data specification in the database is invalid.
	 * @throws IllegalStateException
	 *             If an unexpected exception occurs in any of the parallel
	 *             tasks.
	 */
	private void processTasksInParallel(List<Ethernet> tasks,
			Function<Ethernet, SimpleCallable> mapper) throws StorageException,
			IOException, ProcessException, DataSpecificationException {
		try {
			executor.submitTasks(tasks, mapper).awaitAndCombineExceptions();
		} catch (StorageException | IOException | ProcessException
				| DataSpecificationException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("unexpected exception", e);
		}
	}

	/**
	 * Execute all data specifications that a particular connection knows about,
	 * storing back in the database the information collected about those
	 * executions.
	 *
	 * @param connection
	 *            The handle to the database.
	 * @throws StorageException
	 *             If the database can't be talked to.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws DataSpecificationException
	 *             If a data specification in the database is invalid.
	 * @throws IllegalStateException
	 *             If an unexpected exception occurs in any of the parallel
	 *             tasks.
	 */
	public void loadAllCores(ConnectionProvider<DSEStorage> connection)
			throws StorageException, IOException, ProcessException,
			DataSpecificationException {
		var storage = connection.getStorageInterface();
		var ethernets = storage.listEthernetsToLoad();
		int opsToRun = storage.countWorkRequired();
		try (var bar = new Progress(opsToRun, LOADING_MSG);
				var context = new ExecutionContext(txrx)) {
			processTasksInParallel(ethernets, board -> {
				return () -> loadBoard(board, storage, bar, context);
			});
		}
	}

	/**
	 * Execute all application data specifications that a particular connection
	 * knows about, storing back in the database the information collected about
	 * those executions.
	 *
	 * @param connection
	 *            The handle to the database.
	 * @throws StorageException
	 *             If the database can't be talked to.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws DataSpecificationException
	 *             If a data specification in the database is invalid.
	 * @throws IllegalStateException
	 *             If an unexpected exception occurs in any of the parallel
	 *             tasks.
	 */
	public void loadApplicationCores(ConnectionProvider<DSEStorage> connection)
			throws StorageException, IOException, ProcessException,
			DataSpecificationException {
		var storage = connection.getStorageInterface();
		var ethernets = storage.listEthernetsToLoad();
		int opsToRun = storage.countWorkRequired();
		try (var bar = new Progress(opsToRun, LOADING_MSG);
				var context = new ExecutionContext(txrx)) {
			processTasksInParallel(ethernets, board -> {
				return () -> loadBoard(board, storage, bar, false, context);
			});
		}
	}

	/**
	 * Execute all system data specifications that a particular connection knows
	 * about, storing back in the database the information collected about those
	 * executions.
	 *
	 * @param connection
	 *            The handle to the database.
	 * @throws StorageException
	 *             If the database can't be talked to.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws DataSpecificationException
	 *             If a data specification in the database is invalid.
	 * @throws IllegalStateException
	 *             If an unexpected exception occurs in any of the parallel
	 *             tasks.
	 */
	public void loadSystemCores(ConnectionProvider<DSEStorage> connection)
			throws StorageException, IOException, ProcessException,
			DataSpecificationException {
		var storage = connection.getStorageInterface();
		var ethernets = storage.listEthernetsToLoad();
		int opsToRun = storage.countWorkRequired();
		try (var bar = new Progress(opsToRun, LOADING_MSG);
				var context = new ExecutionContext(txrx)) {
			processTasksInParallel(ethernets, board -> {
				return () -> loadBoard(board, storage, bar, true, context);
			});
		}
	}

	private void loadBoard(Ethernet board, DSEStorage storage, Progress bar,
			ExecutionContext context) throws IOException, ProcessException,
			DataSpecificationException, StorageException {
		try (var c = new BoardLocal(board.location)) {
			var worker = new BoardWorker(board, storage, bar, context);
			for (var ctl : storage.listCoresToLoad(board)) {
				worker.loadCore(ctl);
			}
		}
	}

	private void loadBoard(Ethernet board, DSEStorage storage, Progress bar,
			boolean system, ExecutionContext context) throws IOException,
			ProcessException, DataSpecificationException, StorageException {
		try (var c = new BoardLocal(board.location)) {
			var worker = new BoardWorker(board, storage, bar, context);
			for (var ctl : storage.listCoresToLoad(board, system)) {
				worker.loadCore(ctl);
			}
		}
	}

	@Override
	public void close() throws IOException {
		try {
			txrx.close();
		} catch (IOException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("unexpected failure in close", e);
		}
	}

	private class BoardWorker {
		private final Ethernet board;

		private final DSEStorage storage;

		private final Progress bar;

		private final ExecutionContext context;

		BoardWorker(Ethernet board, DSEStorage storage, Progress bar,
				ExecutionContext context) {
			this.context = context;
			this.board = board;
			this.storage = storage;
			this.bar = bar;
		}

		/**
		 * Execute a data specification and load the results onto a core.
		 *
		 * @param ctl
		 *            The definition of what to run and where to send the
		 *            results.
		 * @throws IOException
		 *             If anything goes wrong with I/O.
		 * @throws ProcessException
		 *             If SCAMP rejects the request.
		 * @throws DataSpecificationException
		 *             If the instructions to build the data are wrong.
		 * @throws StorageException
		 *             If the database access fails.
		 */
		void loadCore(CoreToLoad ctl) throws IOException, ProcessException,
				DataSpecificationException, StorageException {
			ByteBuffer ds;
			try {
				ds = ctl.getDataSpec();
			} catch (StorageException e) {
				throw new DataSpecificationException(
						"failed to read data specification on core " + ctl.core
								+ " of board " + board.location + " ("
								+ board.ethernetAddress + ")",
						e);
			}
			var start = malloc(ctl, ctl.sizeToWrite);
			try (var executor =
					new Executor(ds, machine.getChipAt(ctl.core).sdram)) {
				context.execute(executor, ctl.core, start);
				int size = executor.getConstructedDataSize();
				log.info("loading data onto {} ({} bytes at {})",
						ctl.core.asChipLocation(), toUnsignedLong(size), start);
				int written = APP_PTR_TABLE_BYTE_SIZE;

				for (var reg : executor.regions()) {
					var r = getRealRegionOrNull(reg);
					if (r != null) {
						written += writeRegion(ctl.core, r, r.getRegionBase());
					}
				}

				txrx.writeUser0(ctl.core, start.address);
				bar.update();
				storage.saveLoadingMetadata(ctl, start, size, written);
			} catch (DataSpecificationException e) {
				throw new DataSpecificationException(
						"failed to execute data specification for core "
								+ ctl.core + " of board " + board.location
								+ " (" + board.ethernetAddress + ")",
						e);
			}
		}

		private MemoryLocation malloc(CoreToLoad ctl, int bytesUsed)
				throws IOException, ProcessException {
			return txrx.mallocSDRAM(ctl.core.getScampCore(), bytesUsed,
					new AppID(ctl.appID),
					ctl.core.getP() + CORE_DATA_SDRAM_BASE_TAG);
		}

		/**
		 * Writes the contents of a region. Caller is responsible for ensuring
		 * this method has work to do.
		 *
		 * @param core
		 *            Which core to write to.
		 * @param region
		 *            The region to write.
		 * @param baseAddress
		 *            Where to write the region.
		 * @return How many bytes were actually written.
		 * @throws IOException
		 *             If anything goes wrong with I/O.
		 * @throws ProcessException
		 *             If SCAMP rejects the request.
		 */
		private int writeRegion(HasCoreLocation core, MemoryRegionReal region,
				MemoryLocation baseAddress)
				throws IOException, ProcessException {
			var data = region.getRegionData().duplicate();

			data.flip();
			int written = data.remaining();
			txrx.writeMemory(core.getScampCore(), baseAddress, data);
			return written;
		}
	}

	private static MemoryRegionReal getRealRegionOrNull(MemoryRegion reg) {
		if (!(reg instanceof MemoryRegionReal)) {
			return null;
		}
		var r = (MemoryRegionReal) reg;
		if (r.isUnfilled() || r.getMaxWritePointer() <= 0) {
			return null;
		}
		return r;
	}
}
