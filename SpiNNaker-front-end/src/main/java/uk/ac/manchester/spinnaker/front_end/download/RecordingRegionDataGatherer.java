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

import static java.lang.Integer.toHexString;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class RecordingRegionDataGatherer extends DataGatherer {
	protected static final Logger log =
			getLogger(RecordingRegionDataGatherer.class);
	private final Transceiver txrx;
	private final BufferManagerStorage database;

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

	private Map<RRKey, RecordingRegionsDescriptor> descriptors =
			new HashMap<>();

	private synchronized RecordingRegionsDescriptor getDescriptor(
			ChipLocation chip, int baseAddress)
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
		RecordingRegionsDescriptor descriptor = getDescriptor(chip,
				placement.getVertex().getBaseAddress());
		return new ChannelBufferState(txrx.readMemory(chip,
				descriptor.regionPointers[recordingRegionIndex],
				ChannelBufferState.SIZE));
	}

	private static class RecordingRegion extends Region {
		RecordingRegion(HasCoreLocation core, int regionIndex,
				long from, long to) {
			super(core, regionIndex, (int) from, (int) (to - from));
		}

		@Override
		public String toString() {
			return "RegionRead(@" + core + ":" + regionIndex + ")=0x"
					+ toHexString(startAddress) + "[0x" + toHexString(size)
					+ "]";
		}
	}

	@Override
	protected List<Region> getRegion(Placement placement, int index)
			throws IOException, ProcessException, StorageException {
		ChannelBufferState state = getState(placement, index);
		if (log.isInfoEnabled()) {
			log.info("got state of {}:{} as {}", placement.asCoreLocation(),
					index, state);
		}
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
		if (log.isInfoEnabled()) {
			log.info("generated reads for {}:{} :: {}",
					placement.asCoreLocation(), index, regionPieces);
		}
		/*
		 * But if there are NO reads, directly ask the database to store data so
		 * that it has definitely a record for the current region.
		 */
		if (regionPieces.isEmpty()) {
			database.appendRecordingContents(
					new RecordingRegion(placement, index, state.start, 0),
					new byte[0]);
		}
		return regionPieces;
	}

	@Override
	protected void storeData(Region r, ByteBuffer data)
			throws StorageException {
		if (data == null) {
			log.warn("failed to download data for {}:{} from {}:{}", r.core,
					r.regionIndex, r.startAddress, r.size);
			return;
		}
		log.info("storing region data for {}:{} from {}:{} as {} bytes", r.core,
				r.regionIndex, r.startAddress, r.size, data.remaining());
		database.appendRecordingContents(r, data);
	}
}

/**
 * Describes locations in a recording region. Must match
 * {@code recording_data_t} in {@code recording.c} in SpiNNFrontEndCommon.
 * If the correct branch is merged there. Otherwise, it is defined by
 * pseudo-structure done with offsets into an array of integers. Yay.
 *
 * @author Donal Fellows
 */
final class RecordingRegionsDescriptor {
	/** Number of recording regions. */
	final int numRegions;
	/** Tag for sending buffered output. */
	final int tag;
	/** Destination for sending buffered output. */
	final int tagDestination;
	/** SDP port for receiving buffering messages. */
	final int sdpPort;
	/** Minimum size of data to start buffering out at. */
	final int bufferSizeBeforeRequest;
	/** Minimum interval between buffering out actions. */
	final int timeBetweenTriggers;
	/** Last sequence number used when buffering out. */
	final int lastSequenceNumber;
	/**
	 * Array of addresses of recording regions. This actually points to the
	 * channel's buffer state. Size, {@link #numRegions} entries.
	 */
	final int[] regionPointers;
	/**
	 * Array of sizes of recording regions. Size, {@link #numRegions}
	 * entries.
	 */
	final int[] regionSizes;
	private final ChipLocation chip;
	private final int addr;

	/**
	 * Get an instance of this descriptor from SpiNNaker.
	 *
	 * @param txtx
	 *            The transceiver to use for communications.
	 * @param chip
	 *            The chip to read from.
	 * @param address
	 *            Where on the chip to read the region data from.
	 * @throws IOException
	 *             If I/O fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	RecordingRegionsDescriptor(Transceiver txrx, HasChipLocation chip,
			int address) throws IOException, ProcessException {
		// Read the descriptor from SpiNNaker
		int nr = txrx.readMemory(chip, address, WORD_SIZE).getInt();
		RecordingRegionDataGatherer.log.info("{} recording regions at {} of {}",
				nr, address, chip);
		int size = WORD_SIZE * (REGION_POINTERS_START + 2 * nr);
		ByteBuffer buffer = txrx.readMemory(chip, address, size);
		this.chip = chip.asChipLocation();
		this.addr = address;

		// Unpack the contents of the descriptor
		numRegions = buffer.getInt();
		tag = buffer.getInt();
		tagDestination = buffer.getInt();
		sdpPort = buffer.getInt();
		bufferSizeBeforeRequest = buffer.getInt();
		timeBetweenTriggers = buffer.getInt();
		lastSequenceNumber = buffer.getInt();
		regionPointers = new int[numRegions];
		regionSizes = new int[numRegions];
		IntBuffer intBuffer = buffer.asIntBuffer();
		intBuffer.get(regionPointers);
		intBuffer.get(regionSizes);
	}

	private static final int REGION_POINTERS_START = 7;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("RecordingRegions:")
				.append(chip).append("@0x").append(toHexString(addr))
				.append("{");
		sb.append("#").append(numRegions);
		sb.append(",tag=").append(tag);
		sb.append(",dst=0x").append(toHexString(tagDestination));
		sb.append(",sdp=0x").append(toHexString(sdpPort));
		sb.append(",min=0x").append(toHexString(bufferSizeBeforeRequest));
		sb.append(",trg=").append(timeBetweenTriggers);
		sb.append(",seq=").append(lastSequenceNumber);
		sb.append("}:[0x");
		String sep = "";
		for (int i = 0; i < numRegions; i++) {
			sb.append(sep).append(toHexString(regionPointers[i]))
					.append(":0x").append(toHexString(regionSizes[i]));
			sep = ", 0x";
		}
		return sb.append("]").toString();
	}
}

/**
 * Describes a recording region. Must match {@code recording_channel_t} in
 * {@code recording.c} in SpiNNFrontEndCommon.
 *
 * @author Donal Fellows
 */
class ChannelBufferState {
	/** Size of this structure in bytes. */
	static final int SIZE = 24;
	/** The start buffering area memory address. (32 bits) */
	long start;
	/** The address where data was last written. (32 bits) */
	long currentWrite;
	/** The address where the DMA write got up to. (32 bits) */
	long dmaCurrentWrite;
	/** The address where data was last read. (32 bits) */
	long currentRead;
	/** The address of first byte after the buffer. (32 bits) */
	long end;
	/** The ID of the region. (8 bits) */
	byte regionId;
	/** True if the region overflowed during the simulation. (8 bits) */
	boolean missingInfo;
	/** Last operation performed on the buffer. Read or write (8 bits) */
	boolean lastBufferOperationWasWrite;

	/**
	 * Deserialize an instance of this class.
	 *
	 * @param buffer
	 *            Little-endian buffer to read from. Must be (at least) 24
	 *            bytes long.
	 */
	ChannelBufferState(ByteBuffer buffer) {
		start = Integer.toUnsignedLong(buffer.getInt());
		currentWrite = Integer.toUnsignedLong(buffer.getInt());
		dmaCurrentWrite = Integer.toUnsignedLong(buffer.getInt());
		currentRead = Integer.toUnsignedLong(buffer.getInt());
		end = Integer.toUnsignedLong(buffer.getInt());
		regionId = buffer.get();
		missingInfo = (buffer.get() != 0);
		lastBufferOperationWasWrite = (buffer.get() != 0);
		buffer.get(); // padding
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Channel #").append(regionId)
				.append(":{");
		sb.append("start:0x").append(Long.toHexString(start))
				.append(",currentWrite:0x")
				.append(Long.toHexString(currentWrite))
				.append(",dmaCurrentWrite:0x")
				.append(Long.toHexString(dmaCurrentWrite))
				.append(",currentRead:0x")
				.append(Long.toHexString(currentRead)).append(",end:0x")
				.append(Long.toHexString(end));
		if (missingInfo) {
			sb.append(",missingInfo");
		}
		if (lastBufferOperationWasWrite) {
			sb.append(",lastWasWrite");
		}
		return sb.append("}").toString();
	}
}

/**
 * A simple key class that comprises a chip and a base address.
 *
 * @author Donal Fellows
 */
final class RRKey {
	private final ChipLocation chip;
	private final int baseAddr;

	RRKey(HasChipLocation chip, int baseAddress) {
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
		return baseAddr ^ chip.hashCode();
	}
}
