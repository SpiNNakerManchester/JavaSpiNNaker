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

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_HEADER_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_MEM_REGIONS;
import static uk.ac.manchester.spinnaker.front_end.Constants.PARALLEL_SIZE;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.data_spec.Executor;
import uk.ac.manchester.spinnaker.data_spec.MemoryRegion;
import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor;
import uk.ac.manchester.spinnaker.front_end.BoardLocalSupport;
import uk.ac.manchester.spinnaker.front_end.Progress;
import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.front_end.download.request.Monitor;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.storage.ConnectionProvider;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.storage.DSEStorage.CoreToLoad;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

public class FastExecuteDataSpecification extends BoardLocalSupport
		implements AutoCloseable {
	private static final String LOADING_MSG =
			"loading data specifications onto SpiNNaker";
	private static final Logger log =
			getLogger(FastExecuteDataSpecification.class);
	private static final int REGION_TABLE_SIZE = MAX_MEM_REGIONS * WORD_SIZE;
	private static final int DIRECT_TRANSFER_THRESHOLD = 0; // FIXME

	private final Transceiver txrx;
	private final Map<ChipLocation, Gather> gatherForChip;
	private final Map<ChipLocation, Monitor> monitorForChip;
	private final BasicExecutor executor;
	private final Machine machine;

	public FastExecuteDataSpecification(Machine machine, List<Gather> gathers)
			throws IOException, ProcessException {
		super(machine);
		this.machine = machine;
		executor = new BasicExecutor(PARALLEL_SIZE);
		gatherForChip = new HashMap<>();
		monitorForChip = new HashMap<>();
		for (Gather g : gathers) {
			gatherForChip.put(g.asChipLocation(), g);
			for (Monitor m : g.getMonitors()) {
				gatherForChip.put(m.asChipLocation(), g);
				monitorForChip.put(m.asChipLocation(), m);
			}
		}
		try {
			txrx = new Transceiver(machine);
		} catch (SpinnmanException e) {
			throw new IllegalStateException("failed to talk to BMP, "
					+ "but that shouldn't have happened at all", e);
		}
	}

	public void loadCores(ConnectionProvider<DSEStorage> connection)
			throws StorageException, IOException, ProcessException,
			DataSpecificationException {
		DSEStorage storage = connection.getStorageInterface();
		List<Ethernet> ethernets = storage.listEthernetsToLoad();
		int opsToRun = storage.countWorkRequired();
		try (Progress bar = new Progress(opsToRun, LOADING_MSG)) {
			executor.submitTasks(ethernets.stream()
					.map(board -> () -> loadBoard(board, storage, bar)))
					.awaitAndCombineExceptions();
		} catch (StorageException | IOException | ProcessException
				| DataSpecificationException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("unexpected exception", e);
		}
	}

	private void loadBoard(Ethernet board, DSEStorage storage, Progress bar)
			throws IOException, ProcessException, DataSpecificationException,
			StorageException {
		try (BoardLocal c = new BoardLocal(board.location)) {
			BoardWorker worker = new BoardWorker(board, storage, bar);
			List<CoreToLoad> cores = storage.listCoresToLoad(board, false);
			try (uk.ac.manchester.spinnaker.front_end.dse.FastExecuteDataSpecification.BoardWorker.HighSpeedContext context =
					worker.highSpeedContext()) {
				for (CoreToLoad ctl : cores) {
					log.info("loading data onto {}", ctl.core);
					worker.loadCore(ctl);
				}
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

	private static boolean isToBeIgnored(MemoryRegion r) {
		return r == null || r.isUnfilled() || r.getMaxWritePointer() <= 0;
	}

	private class BoardWorker {
		private IPTag iptag;
		private Ethernet board;
		private DSEStorage storage;
		private Progress bar;

		public BoardWorker(Ethernet board, DSEStorage storage, Progress bar) {
			iptag = gatherForChip.get(board.location).getIptag();
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
		public void loadCore(CoreToLoad ctl) throws IOException,
				ProcessException, DataSpecificationException, StorageException {
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
			try (Executor executor =
					new Executor(ds, machine.getChipAt(ctl.core).sdram)) {
				executor.execute();
				int size = executor.getConstructedDataSize();
				int start = malloc(ctl, size);
				int written = writeHeader(ctl.core, executor, start);

				for (MemoryRegion r : executor.regions()) {
					if (!isToBeIgnored(r)) {
						written += writeRegion(ctl.core, r, r.getRegionBase());
					}
				}

				int user0 = txrx.getUser0RegisterAddress(ctl.core);
				txrx.writeMemory(ctl.core, user0, start);
				bar.update();
				storage.saveLoadingMetadata(ctl, start, size, written);
			} catch (DataSpecificationException e) {
				throw new DataSpecificationException(
						"failed to execute data specification on core "
								+ ctl.core + " of board " + board.location
								+ " (" + board.ethernetAddress + ")",
						e);
			}
		}

		private int malloc(CoreToLoad ctl, int bytesUsed)
				throws IOException, ProcessException {
			return txrx.mallocSDRAM(ctl.core, bytesUsed, new AppID(ctl.appID));
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
		private int writeRegion(CoreLocation core, MemoryRegion region,
				int baseAddress) throws IOException, ProcessException {
			ByteBuffer data = region.getRegionData().duplicate();

			data.flip();
			int written = data.remaining();
			fastWrite(core, baseAddress, data);
			return written;
		}

		class HighSpeedContext implements AutoCloseable {
			HighSpeedContext() {
				throw new RuntimeException("NOT YET IMPLEMENTED"); // FIXME

			}

			@Override
			public void close() throws IOException {
				// TODO Auto-generated method stub

			}

		}

		HighSpeedContext highSpeedContext() { // FIXME better type
			return new HighSpeedContext();
		}

		private void fastWrite(CoreLocation core, int baseAddress,
				ByteBuffer data) throws IOException, ProcessException {
			if (data.remaining() < DIRECT_TRANSFER_THRESHOLD || core
					.onSameChipAs(gatherForChip.get(core.asChipLocation()))) {
				txrx.writeMemory(core, baseAddress, data);
				return;
			}
			// FIXME Auto-generated method stub
		}

	}
}
