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

public class DirectDataGatherer extends DataGatherer {
	private final Transceiver txrx;
	private final Storage database;

	public DirectDataGatherer(Transceiver transceiver, Storage database) {
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

	/**
	 *
	 * @param placement
	 * @param regionID
	 * @return
	 * @throws IOException
	 * @throws ProcessException
	 */
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
