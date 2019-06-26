/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.download;

import static java.lang.Integer.toUnsignedLong;
import static java.lang.Long.toHexString;
import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.front_end.download.request.Placement;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage.Region;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

/**
 * A data gatherer that pulls the data from a recording region.
 *
 * @author Donal Fellows
 */
public class RecordingRegionDataGatherer extends DataGatherer
		implements AutoCloseable {
	/**
	 * How long a termination delay has to be to be worth reporting, in
	 * milliseconds.
	 */
	private static final int TERMINATION_REPORT_THRESHOLD = 250;
	protected static final Logger log =
			getLogger(RecordingRegionDataGatherer.class);
	private final Transceiver txrx;
	private final BufferManagerStorage database;
	private Map<RRKey, RecordingRegionsDescriptor> descriptors =
			new HashMap<>();
	private final ExecutorService dbWorker = newSingleThreadExecutor();
	private int numWrites = 0;

	/**
	 * Create a data gatherer.
	 *
	 * @param transceiver
	 *            How to talk to the machine.
	 * @param machine
	 *            The description of the machine talked to.
	 * @param database
	 *            Where to put the retrieved data.
	 * @throws ProcessException
	 *             If we can't discover the machine details due to SpiNNaker
	 *             rejecting messages
	 * @throws IOException
	 *             If we can't discover the machine details due to I/O problems
	 */
	public RecordingRegionDataGatherer(Transceiver transceiver, Machine machine,
			BufferManagerStorage database)
			throws IOException, ProcessException {
		super(transceiver, machine);
		this.txrx = transceiver;
		this.database = database;
	}

	private synchronized RecordingRegionsDescriptor getDescriptor(
			ChipLocation chip, long baseAddress)
			throws IOException, ProcessException {
		RRKey key = new RRKey(chip, baseAddress);
		RecordingRegionsDescriptor rrd = descriptors.get(key);
		if (rrd == null) {
			rrd = new RecordingRegionsDescriptor(txrx, chip, baseAddress);
			if (log.isDebugEnabled()) {
				log.debug("got recording region info {}", rrd);
			}
			descriptors.put(key, rrd);
		}
		return rrd;
	}

	private ChannelBufferState getState(Placement placement,
			int recordingRegionIndex) throws IOException, ProcessException {
		ChipLocation chip = placement.asChipLocation();
		RecordingRegionsDescriptor descriptor =
				getDescriptor(chip, placement.getVertex().getBaseAddress());
		return new ChannelBufferState(txrx.readMemory(chip,
				descriptor.regionPointers[recordingRegionIndex],
				ChannelBufferState.SIZE));
	}

	@Override
	protected List<Region> getRegion(Placement placement, int index)
			throws IOException, ProcessException {
		ChannelBufferState state = getState(placement, index);
		log.debug("got state of {} R:{} as {}", placement.asCoreLocation(),
				index, state);
		List<Region> regionPieces = new ArrayList<>(2);
		if (state.currentRead < state.currentWrite) {
			regionPieces.add(new RecordingRegion(placement, index,
					state.currentRead, state.currentWrite));
		} else if (state.currentRead > state.currentWrite
				|| state.lastBufferOperationWasWrite) {
			regionPieces.add(new RecordingRegion(placement, index,
					state.currentRead, state.end));
			regionPieces.add(new RecordingRegion(placement, index, state.start,
					state.currentWrite));
		}
		// Remove any zero-sized reads
		regionPieces =
				regionPieces.stream().filter(r -> r.size > 0).collect(toList());
		log.debug("generated reads for {} R:{} :: {}",
				placement.asCoreLocation(), index, regionPieces);
		/*
		 * But if there are NO reads, directly ask the database to store data so
		 * that it has definitely a record for the current region.
		 */
		if (regionPieces.isEmpty()) {
			dbWorker.execute(() -> {
				try {
					database.appendRecordingContents(new RecordingRegion(
							placement, index, state.start, 0), new byte[0]);
					numWrites++;
				} catch (StorageException e) {
					log.error("failed to write to database", e);
				}
			});
		}
		return regionPieces;
	}

	@Override
	protected void storeData(Region r, ByteBuffer data) {
		String addr = toHexString(toUnsignedLong(r.startAddress));
		if (data == null) {
			log.warn("failed to download data for {} R:{} from 0x{}:{}", r.core,
					r.regionIndex, addr, r.size);
			return;
		}
		dbWorker.execute(() -> {
			log.info("storing region data for {} R:{} from 0x{} as {} bytes",
					r.core, r.regionIndex, addr, data.remaining());
			try {
				database.appendRecordingContents(r, data);
				numWrites++;
			} catch (StorageException e) {
				log.error("failed to write to database", e);
			}
		});
	}

	@Override
	public void close() throws InterruptedException {
		log.info("waiting for database usage to complete");
		long start = System.currentTimeMillis();
		dbWorker.shutdown();
		// It really shouldn't take a minute to finish
		dbWorker.awaitTermination(1, MINUTES);
		long end = System.currentTimeMillis();
		log.info("total of {} database writes done", numWrites);
		if (end - start > TERMINATION_REPORT_THRESHOLD) {
			double diff = (end - start) / (double) MSEC_PER_SEC;
			log.info("DB shutdown took {}s", format("%.2f", diff));
		}
	}

	/**
	 * A printable region descriptor.
	 *
	 * @author Donal Fellows
	 */
	private static final class RecordingRegion extends Region {
		RecordingRegion(HasCoreLocation core, int regionIndex, long from,
				long to) {
			super(core, regionIndex, (int) from, (int) (to - from));
		}

		@Override
		public String toString() {
			return format("RegionRead(@%d,%d,%d,%d)=0x%08x[0x%x]",
					core.getX(), core.getY(), core.getP(), regionIndex,
					startAddress, size);
		}
	}

	/**
	 * A simple key class that comprises a chip and a base address.
	 *
	 * @author Donal Fellows
	 */
	private static final class RRKey {
		private final ChipLocation chip;
		private final long baseAddr;

		RRKey(HasChipLocation chip, long baseAddress) {
			this.chip = chip.asChipLocation();
			this.baseAddr = baseAddress;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof RRKey) {
				RRKey o = (RRKey) other;
				return chip.equals(o.chip) && (o.baseAddr == baseAddr);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return ((int) baseAddr) ^ chip.hashCode();
		}
	}
}
