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

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_HEADER_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_MEM_REGIONS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.data_spec.Executor;
import uk.ac.manchester.spinnaker.data_spec.MemoryRegion;
import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.storage.ConnectionProvider;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;
import uk.ac.manchester.spinnaker.storage.DSEStorage.CoreToLoad;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

/**
 * Executes the host based data specification.
 *
 * @author Donal Fellows
 */
public class HostExecuteDataSpecification {
	private static final Logger log = getLogger(HostExecuteDataSpecification.class);
	private static final int REGION_TABLE_SIZE = MAX_MEM_REGIONS * WORD_SIZE;

	/**
	 * Maximum number of parallel threads that can execute and load data
	 * specifications.
	 */
	private static final int PARALLEL_FACTOR = 4;

	/**
	 * Global thread pool for DSE execution.
	 */
	private static ExecutorService executor =
			newFixedThreadPool(PARALLEL_FACTOR);

	private final Machine machine;

	/**
	 * Create a high-level DSE interface.
	 *
	 * @param machine
	 *            The description of the SpiNNaker machine.
	 */
	public HostExecuteDataSpecification(Machine machine) {
		this.machine = machine;
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
	 * @throws InterruptedException
	 *             If the executor is interrupted during use.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws DataSpecificationException
	 *             If a data specification in the database is invalid.
	 * @throws ExecutionException
	 *             If an unexpected exception occurs.
	 */
	public void loadAll(ConnectionProvider connection) throws StorageException,
			IOException, ProcessException, ExecutionException,
			InterruptedException, DataSpecificationException {
		DSEStorage storage = connection.getDSEStorage();
		List<Future<Exception>> tasks = storage.listEthernetsToLoad().stream()
				.map(board -> executor.submit(() -> {
					try (BoardWorker worker = new BoardWorker(board, storage)) {
						for (CoreToLoad ctl : storage.listCoresToLoad(board)) {
							log.info("loading data onto {}", ctl.core);
							worker.loadCore(ctl);
						}
						return null;
					} catch (Exception e) {
						return e;
					}
				})).collect(Collectors.toList());
		List<Exception> fails = new ArrayList<>(tasks.size());
		for (Future<Exception> f : tasks) {
			fails.add(f.get());
		}
		try {
			for (Exception e : fails) {
				if (e != null) {
					throw e;
				}
			}
		} catch (StorageException | IOException | ProcessException
				| DataSpecificationException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("unexpected exception", e);
		}
	}

	private class BoardWorker implements AutoCloseable {
		private final Transceiver txrx;
		private final Ethernet board;
		private final DSEStorage storage;

		BoardWorker(Ethernet board, DSEStorage storage)
				throws IOException, SpinnmanException, ProcessException {
			txrx = new Transceiver(InetAddress.getByName(board.ethernetAddress),
					null);
			this.board = board;
			this.storage = storage;
		}

		/**
		 * Excute a data specification and load the results onto a core.
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
			try (Executor executor = new Executor(ctl.dataSpec,
					machine.getChipAt(ctl.core).sdram)) {
				executor.execute();
				int bytesUsed = executor.getConstructedDataSize();
				int startAddress = txrx.mallocSDRAM(ctl.core, bytesUsed,
						new AppID(ctl.appID));
				int bytesWritten =
						writeHeader(ctl.core, executor, startAddress);

				for (MemoryRegion r : executor.regions()) {
					if (!isToBeIgnored(r)) {
						bytesWritten +=
								writeRegion(ctl.core, r, r.getRegionBase());
					}
				}

				int user0 = txrx.getUser0RegisterAddress(ctl.core);
				txrx.writeMemory(ctl.core, user0, startAddress);
				storage.saveLoadingMetadata(ctl, startAddress, bytesUsed,
						bytesWritten);
			} catch (DataSpecificationException e) {
				throw new DataSpecificationException(
						"failed to execute data specification on core "
								+ ctl.core + " of board " + board.location
								+ " (" + board.ethernetAddress + ")",
						e);
			}
		}

		/**
		 * Writes the header section.
		 *
		 * @param core
		 *            Which core to write to.
		 * @param executor
		 *            The executor which generates the header.
		 * @param startAddress
		 *            Where to write the header.
		 * @return How many bytes were actually written.
		 * @throws IOException
		 *             If anything goes wrong with I/O.
		 * @throws ProcessException
		 *             If SCAMP rejects the request.
		 */
		private int writeHeader(HasCoreLocation core, Executor executor,
				int startAddress) throws IOException, ProcessException {
			ByteBuffer b =
					allocate(APP_PTR_TABLE_HEADER_SIZE + REGION_TABLE_SIZE)
							.order(LITTLE_ENDIAN);

			executor.addHeader(b);
			executor.addPointerTable(b, startAddress);

			b.flip();
			int written = b.remaining();
			txrx.writeMemory(core, startAddress, b);
			return written;
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
		private int writeRegion(HasCoreLocation core, MemoryRegion region,
				int baseAddress) throws IOException, ProcessException {
			ByteBuffer data = region.getRegionData().duplicate();

			data.flip();
			int written = data.remaining();
			txrx.writeMemory(core, baseAddress, data);
			return written;
		}

		@Override
		public void close() throws IOException {
			try {
				txrx.close();
			} catch (IOException | RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new IllegalStateException("unexpected failure in close",
						e);
			}
		}
	}

	private static boolean isToBeIgnored(MemoryRegion r) {
		return r == null || r.isUnfilled() || r.getMaxWritePointer() <= 0;
	}
}
