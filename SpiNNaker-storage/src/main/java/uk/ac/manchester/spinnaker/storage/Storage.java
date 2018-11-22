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
	 * @param run
	 *            Which run to retrieve the data for, or {@code null} to use the
	 *            default (the current run).
	 * @return The region contents.
	 * @throws IllegalArgumentException
	 *             If there's no such saved region.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	byte[] getRegionContents(Region region, Integer run)
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
		return getRegionContents(region, null);
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
	public static class Region {
		public final CoreLocation core;
		public final int regionIndex;
		public final int startAddress;
		public final int size;

		public Region(CoreLocation core, int regionIndex, int startAddress,
				int size) {
			this.core = core;
			this.regionIndex = regionIndex;
			this.startAddress = startAddress;
			this.size = size;
		}

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
	int storeRegionContents(Region region, byte[] contents)
			throws StorageException;

	/**
	 * Stores some bytes in the database. The bytes represent the contents of a
	 * region of a particular SpiNNaker core.
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
		return storeRegionContents(region, ary);
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
}
