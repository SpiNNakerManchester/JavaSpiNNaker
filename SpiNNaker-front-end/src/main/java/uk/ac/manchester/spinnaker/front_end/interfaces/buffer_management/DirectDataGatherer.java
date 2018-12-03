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
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.storage.Storage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

/**
 * A data gatherer that can fetch DSE regions.
 *
 * @author Donal Fellows
 */
public class DirectDataGatherer extends DataGatherer {
	private final Transceiver txrx;
	private final Storage database;

	/**
	 * Create a data gatherer.
	 *
	 * @param transceiver
	 *            How to talk to the machine.
	 * @param database
	 *            Where to put the retrieved data.
	 * @throws ProcessException
	 *             If we can't discover the machine details due to SpiNNaker
	 *             rejecting messages
	 * @throws IOException
	 *             If we can't discover the machine details due to I/O problems
	 */
	public DirectDataGatherer(Transceiver transceiver, Storage database)
			throws IOException, ProcessException {
		super(transceiver);
		this.txrx = transceiver;
		this.database = database;
	}

	private Map<CoreLocation, Map<Integer, ByteBuffer>> coreTableCache =
			new HashMap<>();
	/** The number of memory regions in the DSE model. */
	private static final int MAX_MEM_REGIONS = 16;
	/** Application data magic number. */
	static final int APPDATA_MAGIC_NUM = 0xAD130AD6;
	/** Version of the file produced by the DSE. */
	static final int DSE_VERSION = 0x00010000;

	private IntBuffer getCoreRegionTable(CoreLocation core, Vertex vertex)
			throws IOException, ProcessException {
		// TODO get this info from the database
		Map<Integer, ByteBuffer> map;
		synchronized (coreTableCache) {
			map = coreTableCache.get(core);
			if (map == null) {
				map = new HashMap<>();
				coreTableCache.put(core, map);
			}
		}
		// Individual cores are only ever handled from one thread
		ByteBuffer buffer = map.get(vertex.recordingRegionBaseAddress);
		if (buffer == null) {
			buffer = txrx.readMemory(core, vertex.recordingRegionBaseAddress,
					WORD_SIZE * (MAX_MEM_REGIONS + 2));
			int word = buffer.getInt();
			if (word != APPDATA_MAGIC_NUM) {
				throw new IllegalStateException(
						String.format("unexpected magic number: %08x", word));
			}
			word = buffer.getInt();
			if (word != DSE_VERSION) {
				throw new IllegalStateException(
						String.format("unexpected DSE version: %08x", word));
			}
			map.put(vertex.recordingRegionBaseAddress, buffer);
		}
		return buffer.asIntBuffer();
	}

	@Override
	protected Region getRegion(Placement placement, int regionID)
			throws IOException, ProcessException {
		Region r = new Region();
		r.core = placement.asCoreLocation();
		r.regionID = regionID;
		IntBuffer b = getCoreRegionTable(r.core, placement.vertex);
		r.startAddress = b.get(regionID);
		// TODO This is probably wrong!
		r.size = b.get(regionID + 1) - r.startAddress;
		return r;
	}

	@Override
	protected void storeData(Region r, ByteBuffer data)
			throws StorageException {
		database.storeRegionContents(r.core, r.regionID, data);
	}
}
