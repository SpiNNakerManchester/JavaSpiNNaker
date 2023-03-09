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
package uk.ac.manchester.spinnaker.storage;

import java.nio.ByteBuffer;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/**
 * The interface supported by the storage system.
 *
 * @author Donal Fellows
 */
public interface BufferManagerStorage extends DatabaseAPI {
	/**
	 * Retrieves some bytes from the database. The bytes represent the contents
	 * of a DSE region of a particular SpiNNaker core.
	 *
	 * @param region
	 *            The region descriptor.
	 * @return The region contents.
	 * @throws IllegalArgumentException
	 *             If there's no such saved region.
	 * @throws StorageException
	 *             If anything goes wrong.
	 * @throws UnsupportedOperationException
	 *             This method is unsupported.
	 * @deprecated Currently unsupported; underlying database structure absent
	 */
	@Deprecated
	default byte[] getRegionContents(Region region) throws StorageException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Retrieves some bytes from the database. The bytes represent the contents
	 * of a recording region of a particular SpiNNaker core.
	 *
	 * @param region
	 *            The (DSE) region descriptor.
	 * @return The region contents.
	 * @throws IllegalArgumentException
	 *             If there's no such saved region.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	byte[] getRecordingRegionContents(Region region) throws StorageException;

	/**
	 * Removes some bytes from the database. The bytes represent the contents of
	 * a DSE region of a particular SpiNNaker core.
	 *
	 * @param core
	 *            The core that has the memory region.
	 * @param region
	 *            The region ID.
	 * @throws StorageException
	 *             If anything goes wrong.
	 * @throws UnsupportedOperationException
	 *             This method is unsupported.
	 * @deprecated Currently unsupported; underlying database structure absent
	 */
	@Deprecated
	default void deleteRegionContents(HasCoreLocation core, int region)
			throws StorageException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get a list of all cores that have data stored in the database.
	 * <i>Warning: this is a potentially expensive operation!</i>
	 *
	 * @return A list of cores for which something is stored.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	List<CoreLocation> getCoresWithStorage() throws StorageException;

	/**
	 * Get a list of all regions for a particular core that have data stored in
	 * the database.
	 *
	 * @param core
	 *            The core that has the memory regions.
	 * @return A list of region IDs for which something is stored.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	List<Integer> getRegionsWithStorage(HasCoreLocation core)
			throws StorageException;

	/**
	 * A region descriptor. Not expected to support useful notions of equality.
	 *
	 * @author Donal Fellows
	 */
	class Region {
		private static final int INT_SIZE = 4;

		/**
		 * What core owned the region? Note that the region might be retrieved
		 * from another core of the same chip.
		 */
		@Valid
		public final CoreLocation core;

		/**
		 * What was the index of the region in the table of regions for the
		 * core?
		 */
		public final int regionIndex;

		/**
		 * Where should the data be downloaded from? <em>This is not necessarily
		 * the start of the region.</em>
		 */
		@NotNull
		public final MemoryLocation startAddress;

		/**
		 * How much data should be downloaded? <em>This is not necessarily the
		 * size of the region.</em>
		 */
		@Positive
		public final int size;

		/**
		 * How much data was originally requested?
		 */
		@Positive
		public final int realSize;

		/**
		 * How many extra bytes are being read at the start of the region in
		 * order to get an aligned read?
		 */
		@PositiveOrZero
		public final int initialIgnore;

		/**
		 * How many extra bytes are being read at the end of the region in order
		 * to get an aligned read?
		 */
		@PositiveOrZero
		public final int finalIgnore;

		/**
		 * Create a region descriptor.
		 *
		 * @param core
		 *            What core owned the region? Note that the region might be
		 *            retrieved from another core of the same chip.
		 * @param regionIndex
		 *            What was the index of the region in the table of regions
		 *            for the core?
		 * @param startLocation
		 *            Where should the data be downloaded from? <em>This is not
		 *            necessarily the start of the region.</em>
		 * @param size
		 *            How much data should be downloaded? <em>This is not
		 *            necessarily the size of the region.</em>
		 */
		public Region(HasCoreLocation core, int regionIndex,
				MemoryLocation startLocation, int size) {
			this.core = core.asCoreLocation();
			this.regionIndex = regionIndex;
			realSize = size;
			initialIgnore = startLocation.subWordAlignment();
			size += initialIgnore;
			startAddress = startLocation.add(-initialIgnore);
			// Looks weird, but works
			finalIgnore = (INT_SIZE - (size % INT_SIZE)) % INT_SIZE;
			size += finalIgnore;
			this.size = size;
		}

		/**
		 * @return Whether this is an aligned buffer. Aligned buffers can be
		 *         read with efficient reading techniques.
		 */
		public final boolean isAligned() {
			return initialIgnore == 0 && finalIgnore == 0;
		}

		/** @return Whether this is a non-empty buffer. */
		public final boolean isNonEmpty() {
			return size > 0;
		}

		@Override
		public String toString() {
			return "Region " + regionIndex + " on " + core.toString();
		}
	}

	/**
	 * Stores some bytes in the database. The bytes represent the contents of a
	 * DSE region of a particular SpiNNaker core.
	 *
	 * @param region
	 *            The DSE region that this is the contents of.
	 * @param contents
	 *            The contents to store.
	 * @return The storage ID. (Not currently used elsewhere.)
	 * @throws StorageException
	 *             If anything goes wrong.
	 * @throws UnsupportedOperationException
	 *             This method is unsupported.
	 * @deprecated Currently unsupported; underlying database structure absent
	 */
	@Deprecated
	default int storeDSEContents(Region region, byte[] contents)
			throws StorageException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Stores some bytes in the database. The bytes represent the contents of a
	 * DSE region of a particular SpiNNaker core.
	 *
	 * @param region
	 *            The DSE region that this is the contents of.
	 * @param contents
	 *            The contents to store.
	 * @return The storage ID. (Not currently used elsewhere.)
	 * @throws StorageException
	 *             If anything goes wrong.
	 * @deprecated Currently unsupported; underlying database structure absent
	 */
	@Deprecated
	default int storeRegionContents(Region region, ByteBuffer contents)
			throws StorageException {
		var ary = new byte[contents.remaining()];
		contents.slice().get(ary);
		return storeDSEContents(region, ary);
	}

	/**
	 * Adds some bytes to the database. The bytes represent part of the contents
	 * of a recording region of a particular SpiNNaker core.
	 *
	 * @param region
	 *            The recording region doing the recording.
	 * @param contents
	 *            The bytes to append.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	void appendRecordingContents(Region region, byte[] contents)
			throws StorageException;

	/**
	 * Adds some bytes to the database. The bytes represent part of the contents
	 * of a recording region of a particular SpiNNaker core.
	 *
	 * @param region
	 *            The recording region that is being recorded.
	 * @param contents
	 *            The contents to append.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	default void appendRecordingContents(Region region, ByteBuffer contents)
			throws StorageException {
		var ary = new byte[contents.remaining()];
		contents.slice().get(ary);
		appendRecordingContents(region, ary);
	}
}
