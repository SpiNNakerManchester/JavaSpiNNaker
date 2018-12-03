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
package uk.ac.manchester.spinnaker.storage;

import java.nio.ByteBuffer;
import java.util.List;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * The interface supported by the storage system.
 *
 * @author Donal Fellows
 */
public interface Storage {
	/**
	 * Retrieves some bytes from the database. The bytes represent the contents
	 * of a DSE region of a particular SpiNNaker core.
	 *
	 * @param region
	 *            The region descriptor.
	 * @param reset
	 *            Which reset phase to retrieve the data for, or {@code null} to
	 *            use the default (the current phase).
	 * @param run
	 *            Which run to retrieve the data for within the given reset
	 *            phase, or {@code null} to use the default (the current run).
	 *            It is <em>advised</em> to specify both if either is given.
	 * @return The region contents.
	 * @throws IllegalArgumentException
	 *             If there's no such saved region.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	byte[] getRegionContents(Region region, Integer reset, Integer run)
			throws StorageException;

	/**
	 * Retrieves some bytes from the database. The bytes represent the contents
	 * of a DSE region of a particular SpiNNaker core for the current run.
	 *
	 * @param region
	 *            The region descriptor.
	 * @return The region contents.
	 * @throws IllegalArgumentException
	 *             If there's no such saved region.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	default byte[] getRegionContents(Region region) throws StorageException {
		return getRegionContents(region, null, null);
	}

	/**
	 * Retrieves some bytes from the database. The bytes represent the contents
	 * of a recording region of a particular SpiNNaker core.
	 *
	 * @param region
	 *            The (DSE) region descriptor.
	 * @param recordingIndex
	 *            The index into the recordings done by that core.
	 * @param run
	 *            Which run to retrieve the data for, or {@code null} to use the
	 *            default (the current run).
	 * @return The region contents.
	 * @throws IllegalArgumentException
	 *             If there's no such saved region.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	byte[] getRecordingRegionContents(Region region, int recordingIndex,
			Integer run) throws StorageException;

	/**
	 * Retrieves some bytes from the database. The bytes represent the contents
	 * of a recording region of a particular SpiNNaker core for the current run.
	 *
	 * @param region
	 *            The (DSE) region descriptor.
	 * @param recordingIndex
	 *            The index into the recordings done by that core.
	 * @return The region contents.
	 * @throws IllegalArgumentException
	 *             If there's no such saved region.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	default byte[] getRecordingRegionContents(Region region, int recordingIndex)
			throws StorageException {
		return getRecordingRegionContents(region, recordingIndex, null);
	}

	/**
	 * Removes some bytes from the database. The bytes represent the contents of
	 * a DSE region of a particular SpiNNaker core.
	 *
	 * @param core
	 *            The core that has the memory region.
	 * @param region
	 *            The region ID.
	 * @param run
	 *            Which run to delete the data for, or {@code null} to use the
	 *            default (the current run).
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	void deleteRegionContents(HasCoreLocation core, int region, Integer run)
			throws StorageException;

	/**
	 * Removes some bytes from the database. The bytes represent the contents of
	 * a DSE region of a particular SpiNNaker core in the current run.
	 *
	 * @param core
	 *            The core that has the memory region.
	 * @param region
	 *            The region ID.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	default void deleteRegionContents(HasCoreLocation core, int region)
			throws StorageException {
		deleteRegionContents(core, region, null);
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
	 * A DSE region descriptor.
	 *
	 * @author Donal Fellows
	 */
	class Region {
		/**
		 * What core owned the DSE region? Note that the region might be
		 * retrieved from another core of the same chip.
		 */
		public final CoreLocation core;
		/**
		 * What was the index of the DSE region in the table of regions for the
		 * core?
		 */
		public final int regionIndex;
		/**
		 * Where should the data be downloaded from? <em>This is not necessarily
		 * the start of the region, or even inside the actual allocation for
		 * that region.</em> (It varies when the DSE region is associated with
		 * the recording mechanism, for example.)
		 */
		public final int startAddress;
		/**
		 * How much data should be downloaded? <em>This is not necessarily the
		 * size of the region, or even constrained by the actual allocation for
		 * that region.</em> (It varies when the DSE region is associated with
		 * the recording mechanism, for example.)
		 */
		public final int size;

		/**
		 * Create a DSE region descriptor.
		 *
		 * @param core
		 *            What core owned the DSE region? Note that the region might
		 *            be retrieved from another core of the same chip.
		 * @param regionIndex
		 *            What was the index of the DSE region in the table of
		 *            regions for the core?
		 * @param startAddress
		 *            Where should the data be downloaded from? <em>This is not
		 *            necessarily the start of the region, or even inside the
		 *            actual allocation for that region.</em> (It varies when
		 *            the DSE region is associated with the recording mechanism,
		 *            for example.)
		 * @param size
		 *            How much data should be downloaded? <em>This is not
		 *            necessarily the size of the region, or even constrained by
		 *            the actual allocation for that region.</em> (It varies
		 *            when the DSE region is associated with the recording
		 *            mechanism, for example.)
		 */
		public Region(CoreLocation core, int regionIndex, int startAddress,
				int size) {
			this.core = core;
			this.regionIndex = regionIndex;
			this.startAddress = startAddress;
			this.size = size;
		}

		/**
		 * Create a DSE region descriptor.
		 *
		 * @param core
		 *            What core owned the DSE region? Note that the region might
		 *            be retrieved from another core of the same chip.
		 * @param regionIndex
		 *            What was the index of the DSE region in the table of
		 *            regions for the core?
		 * @param startAddress
		 *            Where should the data be downloaded from? <em>This is not
		 *            necessarily the start of the region, or even inside the
		 *            actual allocation for that region.</em> (It varies when
		 *            the DSE region is associated with the recording mechanism,
		 *            for example.)
		 * @param size
		 *            How much data should be downloaded? <em>This is not
		 *            necessarily the size of the region, or even constrained by
		 *            the actual allocation for that region.</em> (It varies
		 *            when the DSE region is associated with the recording
		 *            mechanism, for example.)
		 */
		public Region(HasCoreLocation core, int regionIndex, int startAddress,
				int size) {
			this(core.asCoreLocation(), regionIndex, startAddress, size);
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
	 */
	int storeDSEContents(Region region, byte[] contents)
			throws StorageException;

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
	 */
	default int storeRegionContents(Region region, ByteBuffer contents)
			throws StorageException {
		byte[] ary = new byte[contents.remaining()];
		contents.slice().get(ary);
		return storeDSEContents(region, ary);
	}

	/**
	 * Adds some bytes to the database. The bytes represent part of the contents
	 * of a recording region of a particular SpiNNaker core.
	 *
	 * @param region
	 *            The DSE region owning the recording.
	 * @param recordingIndex
	 *            The index of this recording.
	 * @param contents
	 *            The bytes to append.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	void appendRecordingContents(Region region, int recordingIndex,
			byte[] contents) throws StorageException;

	/**
	 * Adds some bytes to the database. The bytes represent part of the contents
	 * of a recording region of a particular SpiNNaker core.
	 *
	 * @param region
	 *            The DSE region that this is the contents of.
	 * @param recordingIndex
	 *            The index of this recording.
	 * @param contents
	 *            The contents to store.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	default void appendRecordingContents(Region region, int recordingIndex,
			ByteBuffer contents) throws StorageException {
		byte[] ary = new byte[contents.remaining()];
		contents.slice().get(ary);
		appendRecordingContents(region, recordingIndex, ary);
	}

	/**
	 * Note that a particular core is a machine graph vertex, and that that
	 * vertex uses the recording interface.
	 *
	 * @param region
	 *            The information about where we're talking about.
	 * @param label
	 *            The label of the vertex.
	 * @return The ID of the vertex.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	int noteRecordingVertex(Region region, String label)
			throws StorageException;
}
