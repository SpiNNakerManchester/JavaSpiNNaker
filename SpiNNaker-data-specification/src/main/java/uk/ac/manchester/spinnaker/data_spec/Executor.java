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
package uk.ac.manchester.spinnaker.data_spec;

import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableCollection;
import static org.apache.commons.io.FileUtils.openInputStream;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APPDATA_MAGIC_NUM;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_BYTE_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.DSE_VERSION;
import static uk.ac.manchester.spinnaker.data_spec.Constants.END_SPEC_EXECUTOR;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_MEM_REGIONS;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;

/**
 * Used to execute a SpiNNaker data specification language file to produce a
 * memory image.
 *
 * @author Donal Fellows
 */
public class Executor implements Closeable {
	private static final Logger log = getLogger(Executor.class);

	private InputStream inputStream;

	private final ByteBuffer input;

	private final Functions funcs;

	/** The collection of memory regions that can be written to. */
	private final MemoryRegionCollection memRegions;

	/**
	 * Create an executor.
	 *
	 * @param inputFile
	 *            The file to read the specification from
	 * @param memorySpace
	 *            memory available on the destination architecture
	 * @throws IOException
	 *             If a problem happens when reading the file
	 */
	public Executor(File inputFile, int memorySpace) throws IOException {
		this(openInputStream(inputFile), memorySpace);
	}

	/**
	 * Create an executor.
	 *
	 * @param inputStream
	 *            The object to read the specification language file from
	 * @param memorySpace
	 *            memory available on the destination architecture
	 * @throws IOException
	 *             If a problem happens when reading the input stream
	 */
	public Executor(InputStream inputStream, int memorySpace)
			throws IOException {
		this(wrap(toByteArray(inputStream)), memorySpace);
		this.inputStream = inputStream;
	}

	/**
	 * Create an executor.
	 *
	 * @param input
	 *            The object to read the specification language file from
	 * @param memorySpace
	 *            memory available on the destination architecture
	 */
	public Executor(ByteBuffer input, int memorySpace) {
		this.input = input.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		this.input.rewind(); // Ensure we start from the beginning
		memRegions = new MemoryRegionCollection(MAX_MEM_REGIONS);
		funcs = new Functions(this.input, memorySpace, memRegions);
		if (log.isDebugEnabled()) {
			logInput();
		}
	}

	private void logInput() {
		IntBuffer b = input.asIntBuffer();
		int[] a = new int[b.limit()];
		b.get(a);
		if (log.isDebugEnabled()) {
			log.debug("processing input: {}",
					stream(a).mapToObj(Integer::toHexString)
							.collect(Collectors.toList()));
		}
	}

	/**
	 * @throws IOException
	 *             if the spec is being read from a stream and a close of the
	 *             stream fails.
	 */
	@Override
	public void close() throws IOException {
		if (inputStream != null) {
			inputStream.close();
			inputStream = null;
		}
	}

	/**
	 * Executes the specification.
	 *
	 * @throws DataSpecificationException
	 *             if anything goes wrong
	 */
	public void execute() throws DataSpecificationException {
		while (true) {
			int index = input.position();
			int cmd = input.getInt();
			Callable instruction = funcs.getOperation(cmd, index);
			if (END_SPEC_EXECUTOR == instruction.execute(cmd)) {
				break;
			}
		}
	}

	/**
	 * Get how much space was allocated overall. Only useful after a
	 * specification has been executed.
	 *
	 * @return The total space allocated, not including any header or region
	 *         address table.
	 */
	public int getTotalSpaceAllocated() {
		return funcs.spaceAllocated;
	}

	/**
	 * Get the memory region with a particular ID.
	 *
	 * @param regionID
	 *            The ID to look up.
	 * @return The memory region with that ID.
	 */
	public MemoryRegion getRegion(int regionID) {
		return memRegions.get(regionID);
	}

	/**
	 * Set the base address of the data and update the region addresses.
	 *
	 * @param startAddress The base address to set.
	 */
	public void setBaseAddress(int startAddress) {
		int nextOffset = APP_PTR_TABLE_BYTE_SIZE;
		for (MemoryRegion reg : memRegions) {
			if (reg instanceof MemoryRegionReal) {
				MemoryRegionReal r = (MemoryRegionReal) reg;
				r.setRegionBase(nextOffset + startAddress);
				nextOffset += r.getAllocatedSize();
			}
		}
	}

	/**
	 * Get the header of the data added to a buffer.
	 *
	 * @param buffer
	 *            The buffer to write into.
	 */
	public void addHeader(ByteBuffer buffer) {
		assert buffer.order() == LITTLE_ENDIAN;
		buffer.putInt(APPDATA_MAGIC_NUM);
		buffer.putInt(DSE_VERSION);
	}

	/**
	 * Get the pointer table stored in a buffer.
	 *
	 * @param buffer
	 *            The buffer to store it in
	 */
	public void addPointerTable(ByteBuffer buffer) {
		assert buffer.order() == LITTLE_ENDIAN;
		for (MemoryRegion reg : memRegions) {
			if (reg != null) {
				buffer.putInt(reg.getRegionBase());
				if (reg instanceof MemoryRegionReal) {
					// Work out the checksum
					MemoryRegionReal regReal = (MemoryRegionReal) reg;
					int n_words = (int) Math.ceil(regReal.getMaxWritePointer()
							/ 4);
					IntBuffer buf = regReal.getRegionData().duplicate()
							.order(LITTLE_ENDIAN).rewind().asIntBuffer();
					long sum = 0;
					for (int i = 0; i < n_words; i++) {
						sum = (sum + (buf.get() & 0xFFFFFFFFL)) & 0xFFFFFFFFL;
					}
					// Write the checksum and number of words
					buffer.putInt((int) (sum & 0xFFFFFFFFL));
					buffer.putInt(n_words);
				} else {
					// Don't checksum references
					buffer.putInt(0);
					buffer.putInt(0);
				}
			} else {
				// There is no data for non-regions
				buffer.putInt(0);
				buffer.putInt(0);
				buffer.putInt(0);
			}
		}
	}

	/** @return the size of the data that will be written to memory. */
	public int getConstructedDataSize() {
		return APP_PTR_TABLE_BYTE_SIZE + memRegions.stream()
				.filter(r -> r instanceof MemoryRegionReal)
				.mapToInt(r -> ((MemoryRegionReal) r).getAllocatedSize()).sum();
	}

	/**
	 * Get the regions of the executor as an unmodifiable iterable.
	 *
	 * @return The regions.
	 */
	public Collection<MemoryRegion> regions() {
		return unmodifiableCollection(memRegions);
	}

	/**
	 * Get the regions marked as referenceable during execution.
	 *
	 * @return The region IDs.
	 */
	public Collection<Integer> getReferenceableRegions() {
		return unmodifiableCollection(funcs.getReferenceableRegions());
	}

	/**
	 * Get the regions that are references to others.
	 *
	 * @return The region IDs.
	 */
	public Collection<Integer> getRegionsToFill() {
		return unmodifiableCollection(funcs.getRegionsToFill());
	}
}
