/*
 * Copyright (c) 2018-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.download;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.front_end.download.RecordingRegion.getRecordingRegionDescriptors;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.front_end.download.request.Placement;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage.Region;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * A data gatherer that pulls the data from a recording region. Internally, this
 * accepts requests to store data (after it has been retrieved from SpiNNaker)
 * in parallel and passes them to a worker thread so that only that thread needs
 * to hold a write transaction open.
 *
 * @author Donal Fellows
 */
public class RecordingRegionDataGatherer extends DataGatherer {
	/**
	 * How long a termination delay has to be to be worth reporting, in
	 * milliseconds.
	 */
	private static final int TERMINATION_REPORT_THRESHOLD = 250;

	private static final Logger log =
			getLogger(RecordingRegionDataGatherer.class);

	private final BufferManagerStorage database;

	@GuardedBy("itself")
	private final Map<Placement, List<RecordingRegion>> recordingRegions =
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
	@MustBeClosed
	public RecordingRegionDataGatherer(TransceiverInterface transceiver,
			Machine machine, BufferManagerStorage database)
			throws IOException, ProcessException {
		super(transceiver, machine);
		this.database = database;
	}

	private List<RecordingRegion> getRegions(Placement placement)
			throws IOException, ProcessException, InterruptedException {
		// Cheap check first
		synchronized (recordingRegions) {
			var regions = recordingRegions.get(placement);
			if (regions != null) {
				return regions;
			}
		}

		// Need to go to the machine; don't hold the lock while doing so
		var regions = getRecordingRegionDescriptors(txrx, placement);
		synchronized (recordingRegions) {
			// Put the value in the map if it wasn't already there
			return recordingRegions.computeIfAbsent(placement, __ -> regions);
		}
	}

	@Override
	protected List<Region> getRegion(Placement placement, int index)
			throws IOException, ProcessException, InterruptedException {
		var region = getRegions(placement).get(index);
		log.debug("got region of {} R:{} as {}", placement.asCoreLocation(),
				index, region);
		if (region.size > 0) {
			return List.of(new Region(placement, index, region.data,
					(int) region.size));
		}
		return List.of();
	}

	@Override
	protected void storeData(Region r, ByteBuffer data) {
		if (data == null) {
			log.warn("failed to download data for {} R:{} from {}:{}", r.core,
					r.regionIndex, r.startAddress, r.size);
			return;
		}
		dbWorker.execute(() -> {
			log.info("storing region data for {} R:{} from {} as {} bytes",
					r.core, r.regionIndex, r.startAddress, data.remaining());
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
		super.close();
		log.info("waiting for database usage to complete");
		long start = currentTimeMillis();
		dbWorker.shutdown();
		// It really shouldn't take a minute to finish
		dbWorker.awaitTermination(1, MINUTES);
		long end = currentTimeMillis();
		log.info("total of {} database writes done", numWrites);
		if (end - start > TERMINATION_REPORT_THRESHOLD) {
			double diff = (end - start) / (double) MSEC_PER_SEC;
			log.info("DB shutdown took {}s", format("%.2f", diff));
		}
	}
}
