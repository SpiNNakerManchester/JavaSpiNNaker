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
package uk.ac.manchester.spinnaker.front_end.download;

import static java.lang.String.format;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.front_end.download.request.Placement;
import uk.ac.manchester.spinnaker.front_end.download.request.Vertex;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage.Region;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * A data gatherer that can fetch DSE regions.
 *
 * @author Donal Fellows
 * @deprecated This class uses an unimplemented API call that needs to be fixed
 *             but hasn't been yet. It also assumes that there are no shared
 *             regions.
 * @see RecordingRegionDataGatherer
 */
@Deprecated
public final class DirectDataGatherer extends DataGatherer {
	/** The number of memory regions in the DSE model. */
	private static final int MAX_MEM_REGIONS = 16;

	/** Application data magic number. */
	private static final int APPDATA_MAGIC_NUM = 0xAD130AD6;

	/** Version of the file produced by the DSE. */
	private static final int DSE_VERSION = 0x00010000;

	private final BufferManagerStorage database;

	@GuardedBy("itself")
	private final Map<CoreLocation,
			Map<MemoryLocation, ByteBuffer>> coreTableCache;

	/**
	 * Create a data gatherer.
	 *
	 * @param transceiver
	 *            How to talk to the machine.
	 * @param database
	 *            Where to put the retrieved data.
	 * @param machine
	 *            The description of the machine being talked to.
	 * @throws ProcessException
	 *             If we can't discover the machine details due to SpiNNaker
	 *             rejecting messages
	 * @throws IOException
	 *             If we can't discover the machine details due to I/O problems
	 */
	@MustBeClosed
	public DirectDataGatherer(TransceiverInterface transceiver, Machine machine,
			BufferManagerStorage database)
			throws IOException, ProcessException {
		super(transceiver, machine);
		this.database = database;
		coreTableCache = new HashMap<>();
	}

	/**
	 * Get the region location table for a chip.
	 *
	 * @param core
	 *            Where to retrieve from.
	 * @param vertex
	 *            Information about what this means.
	 * @return The region location table, as an integer buffer.
	 * @throws IOException
	 *             If IO fails
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	private IntBuffer getCoreRegionTable(CoreLocation core, Vertex vertex)
			throws IOException, ProcessException, InterruptedException {
		// TODO get this info from the database, if the DB knows it
		Map<MemoryLocation, ByteBuffer> map;
		synchronized (coreTableCache) {
			map = coreTableCache.computeIfAbsent(core, __ -> new HashMap<>());
		}
		// Individual cores are only ever handled from one thread
		var buffer = map.get(vertex.base());
		if (buffer == null) {
			buffer = txrx.readMemory(core, vertex.base(),
					WORD_SIZE * (MAX_MEM_REGIONS + 2));
			int word = buffer.getInt();
			if (word != APPDATA_MAGIC_NUM) {
				throw new IllegalStateException(
						format("unexpected magic number: %08x", word));
			}
			word = buffer.getInt();
			if (word != DSE_VERSION) {
				throw new IllegalStateException(
						format("unexpected DSE version: %08x", word));
			}
			map.put(vertex.base(), buffer);
		}
		return buffer.asIntBuffer();
	}

	@Override
	protected List<Region> getRegion(Placement placement, int regionID)
			throws IOException, ProcessException, InterruptedException {
		var b = getCoreRegionTable(placement.asCoreLocation(),
				placement.vertex());
		// TODO This is wrong because of shared regions!
		int size = b.get(regionID + 1) - b.get(regionID);
		return List.of(new Region(placement, regionID,
				new MemoryLocation(b.get(regionID)), size));
	}

	@Override
	protected void storeData(Region r, ByteBuffer data)
			throws StorageException {
		database.storeRegionContents(r, data);
	}
}
