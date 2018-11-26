package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.storage.Storage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

/**
 * A data gatherer that pulls the data from a recording region.
 *
 * @author Donal Fellows
 */
public class RecordingRegionDataGatherer extends DataGatherer {
	private final Transceiver txrx;
	private final Storage database;

	public RecordingRegionDataGatherer(Transceiver transceiver,
			Storage database) throws IOException, ProcessException {
		super(transceiver);
		this.txrx = transceiver;
		this.database = database;
	}

	/**
	 * Describes locations in a recording region. Must match
	 * {@code recording_data_t} in {@code recording.c} in SpiNNFrontEndCommon.
	 * If the correct branch is merged there. Otherwise, it is defined by
	 * pseudo-structure done with offsets into an array of integers. Yay.
	 *
	 * @author Donal Fellows
	 */
	static class RecordingRegionsDescriptor {
		int numRegions;
		int tag;
		int tagDestination;
		int sdpPort;
		int bufferSizeBeforeRequest;
		int timeBetweenTriggers;
		int lastSequenceNumber;
		int[] regionPointers;
		int[] regionSizes;

		private RecordingRegionsDescriptor(int numRegions, ByteBuffer buffer) {
			this.numRegions = numRegions;
			tag = buffer.getInt();
			tagDestination = buffer.getInt();
			sdpPort = buffer.getInt();
			bufferSizeBeforeRequest = buffer.getInt();
			lastSequenceNumber = buffer.getInt();
			regionPointers = new int[numRegions];
			buffer.asIntBuffer().get(regionPointers);
			regionSizes = new int[numRegions];
			buffer.asIntBuffer().get(regionSizes);
		}

		private static final int REGION_POINTERS_START = 7;

		static RecordingRegionsDescriptor get(Transceiver txrx,
				HasChipLocation chip, int address)
				throws IOException, ProcessException {
			int numRegions = txrx.readMemory(chip, address, WORD_SIZE).getInt();
			int size = WORD_SIZE * (REGION_POINTERS_START + 2 * numRegions);
			return new RecordingRegionsDescriptor(numRegions, txrx
					.readMemory(chip, address + WORD_SIZE, size - WORD_SIZE));
		}
	}

	/**
	 * Describes a recording region. Must match {@code recording_channel_t} in
	 * {@code recording.c} in SpiNNFrontEndCommon.
	 *
	 * @author Donal Fellows
	 */
	static class ChannelBufferState {
		/** Size of this structure in bytes. */
		static final int SIZE = 24;
		/** The start buffering area memory address. (32 bits) */
		int start;
		/** The address where data was last written. (32 bits) */
		int currentWrite;
		/** The address where the DMA write got up to. (32 bits) */
		int dmaCurrentWrite;
		/** The address where data was last read. (32 bits) */
		int currentRead;
		/** The address of first byte after the buffer. (32 bits) */
		int end;
		/** The ID of the region. (8 bits) */
		byte regionId;
		/** True if the region overflowed during the simulation. (8 bits) */
		boolean missingInfo;
		/** Last operation performed on the buffer. Read or write (8 bits) */
		boolean lastBufferOperationWasWrite;

		ChannelBufferState(ByteBuffer buffer) {
			start = buffer.getInt();
			currentWrite = buffer.getInt();
			dmaCurrentWrite = buffer.getInt();
			currentRead = buffer.getInt();
			end = buffer.getInt();
			regionId = buffer.get();
			missingInfo = (buffer.get() != 0);
			lastBufferOperationWasWrite = (buffer.get() != 0);
			buffer.get(); // padding
		}
	}

	private Map<ChipLocation, RecordingRegionsDescriptor> descriptors =
			new HashMap<>();

	private synchronized RecordingRegionsDescriptor getDescriptor(
			ChipLocation chip, int baseAddress)
			throws IOException, ProcessException {
		RecordingRegionsDescriptor rrd = descriptors.get(chip);
		if (rrd == null) {
			rrd = RecordingRegionsDescriptor.get(txrx, chip, baseAddress);
			descriptors.put(chip, rrd);
		}
		return rrd;
	}

	private ChannelBufferState getState(Placement placement, int regionID)
			throws IOException, ProcessException {
		ChipLocation chip = placement.asChipLocation();
		RecordingRegionsDescriptor descriptor = getDescriptor(chip,
				placement.getVertex().recordingRegionBaseAddress);
		return new ChannelBufferState(txrx.readMemory(chip,
				descriptor.regionPointers[regionID], ChannelBufferState.SIZE));
	}

	@Override
	protected Region getRegion(Placement placement, int regionID)
			throws IOException, ProcessException {
		ChannelBufferState state = getState(placement, regionID);
		Region r = new Region();
		r.core = placement.asCoreLocation();
		r.regionID = state.regionId;
		r.startAddress = state.start;
		r.size = state.end - state.start;
		return r;
	}

	@Override
	protected void storeData(Region r, ByteBuffer data)
			throws StorageException {
		database.appendRegionContents(r.core, r.regionID, data);
	}
}
