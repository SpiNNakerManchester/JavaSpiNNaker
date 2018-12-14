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
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_HEADER_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_MEM_REGIONS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import uk.ac.manchester.spinnaker.data_spec.Executor;
import uk.ac.manchester.spinnaker.data_spec.MemoryRegion;
import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.storage.ConnectionProvider;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Board;
import uk.ac.manchester.spinnaker.storage.DSEStorage.CoreToLoad;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

/**
 * Executes the host based data specification.
 *
 * @author Donal Fellows
 */
public class HostExecuteDataSpecification {
	private static final int REGION_TABLE_SIZE = MAX_MEM_REGIONS * WORD_SIZE;
	private static final int PARALLEL_FACTOR = 4;
	private static ExecutorService executor =
			newFixedThreadPool(PARALLEL_FACTOR);
	private Transceiver txrx;
	private Machine machine;

	/**
	 * Create a high-level DSE interface.
	 *
	 * @param transceiver
	 *            The transceiver used to do the communication.
	 * @throws ProcessException
	 *             If we couldn't discover the machine description from the
	 *             transceiver due to SpiNNaker rejecting messages.
	 * @throws IOException
	 *             If the transceiver's communications fail.
	 */
	public HostExecuteDataSpecification(Transceiver transceiver)
			throws IOException, ProcessException {
		this.txrx = transceiver;
		machine = txrx.getMachineDetails();
	}

	/**
	 * Create a high-level DSE interface.
	 *
	 * @param transceiver
	 *            The transceiver used to do the communication.
	 * @param machine
	 *            The description of the machine we are talking to.
	 */
	public HostExecuteDataSpecification(Transceiver transceiver,
			Machine machine) {
		this.txrx = transceiver;
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
		List<Exception> fails = new ArrayList<>();
		List<Future<Exception>> tasks = storage.listBoardsToLoad().stream()
				.map(board -> executor.submit(() -> {
					try {
						for (CoreToLoad ctl : storage.listCoresToLoad(board)) {
							loadCore(storage, board, ctl);
						}
						return null;
					} catch (Exception e) {
						return e;
					}
				})).collect(Collectors.toList());
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

	private void loadCore(DSEStorage storage, Board board, CoreToLoad ctl)
			throws IOException, ProcessException, DataSpecificationException,
			StorageException {
		try (Executor executor =
				new Executor(ctl.dataSpec, machine.getChipAt(ctl.core).sdram)) {
			executor.execute();
			int bytesUsed = executor.getConstructedDataSize();
			int startAddress = txrx.mallocSDRAM(ctl.core, bytesUsed, ctl.appID);
			int bytesWritten = writeHeader(ctl.core, executor, startAddress);

			for (MemoryRegion r : executor.regions()) {
				if (!isToBeIgnored(r)) {
					bytesWritten += writeRegion(ctl.core, r, r.getRegionBase());
				}
			}

			int user0 = txrx.getUser0RegisterAddress(ctl.core);
			txrx.writeMemory(ctl.core, user0, startAddress);
			storage.saveLoadingMetadata(ctl, startAddress, bytesUsed,
					bytesWritten);
		} catch (DataSpecificationException e) {
			throw new DataSpecificationException(
					"failed to execute data specification on core " + ctl.core
							+ " of board " + board.ethernet + " ("
							+ board.ethernetAddress + ")", e);
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
		ByteBuffer b = allocate(APP_PTR_TABLE_HEADER_SIZE + REGION_TABLE_SIZE)
				.order(LITTLE_ENDIAN);

		executor.addHeader(b);
		executor.addPointerTable(b, startAddress);

		b.flip();
		int written = b.remaining();
		txrx.writeMemory(core, startAddress, b);
		return written;
	}

	/**
	 * Writes the contents of a region. Caller is responsible for ensuring this
	 * method has work to do.
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

	private static boolean isToBeIgnored(MemoryRegion r) {
		return r == null || r.isUnfilled() || r.getMaxWritePointer() <= 0;
	}
}
