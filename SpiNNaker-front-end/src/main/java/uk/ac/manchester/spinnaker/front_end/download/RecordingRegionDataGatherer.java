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
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.front_end.download.RecordingRegion.getRecordingRegionDescriptors;
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
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage.Region;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/**
 * A data gatherer that pulls the data from a recording region. Internally, this
 * accepts requests to store data (after it has been retrieved from SpiNNaker)
 * in parallel and passes them to a worker thread so that only that thread needs
 * to hold a write transaction open.
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

	private Map<Placement, List<RecordingRegion>> recordingRegions =
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

	private List<RecordingRegion> getRegions(Placement placement)
			throws IOException, ProcessException {
		// Cheap check first
		synchronized (recordingRegions) {
			List<RecordingRegion> regions = recordingRegions.get(placement);
			if (regions != null) {
				return regions;
			}
		}

		// Need to go to the machine; don't hold the lock while doing so
		List<RecordingRegion> regions =
				getRecordingRegionDescriptors(txrx, placement);
		synchronized (recordingRegions) {
			// Put the value in the map if it wasn't already there
			return recordingRegions.computeIfAbsent(placement, key -> regions);
		}
	}

	@Override
	protected List<Region> getRegion(Placement placement, int index)
			throws IOException, ProcessException {
		RecordingRegion region = getRegions(placement).get(index);
		log.debug("got region of {} R:{} as {}", placement.asCoreLocation(),
				index, region);
		List<Region> regionPieces = new ArrayList<>(1);
		if (region.size > 0) {
			regionPieces.add(new Region(placement, index, (int) region.data,
					(int) region.size));
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
}
