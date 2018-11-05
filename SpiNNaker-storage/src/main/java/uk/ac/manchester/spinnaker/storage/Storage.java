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
	 * Stores some bytes in the database. The bytes represent the contents of a
	 * region of a particular SpiNNaker core.
	 *
	 * @param core
	 *            The core that has the memory region.
	 * @param region
	 *            The region ID.
	 * @param contents
	 *            The contents to store.
	 * @return The row ID. (Not currently used elsewhere.)
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	int storeRegionContents(HasCoreLocation core, int region, byte[] contents)
			throws StorageException;

	/**
	 * Stores some bytes in the database. The bytes represent the contents of a
	 * region of a particular SpiNNaker core.
	 *
	 * @param core
	 *            The core that has the memory region.
	 * @param region
	 *            The region ID.
	 * @param contents
	 *            The contents to store.
	 * @return The row ID. (Not currently used elsewhere.)
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	default int storeRegionContents(HasCoreLocation core, int region,
			ByteBuffer contents) throws StorageException {
		byte[] ary = new byte[contents.remaining()];
		contents.slice().get(ary);
		return storeRegionContents(core, region, ary);
	}

	/**
	 * Appends some bytes to some already in the database. The bytes represent
	 * the contents of a region of a particular SpiNNaker core.
	 *
	 * @param core
	 *            The core that has the memory region.
	 * @param region
	 *            The region ID.
	 * @param contents
	 *            The contents to store.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	void appendRegionContents(HasCoreLocation core, int region, byte[] contents)
			throws StorageException;

	/**
	 * Appends some bytes to some already in the database. The bytes represent
	 * the contents of a region of a particular SpiNNaker core.
	 *
	 * @param core
	 *            The core that has the memory region.
	 * @param region
	 *            The region ID.
	 * @param contents
	 *            The contents to store.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	default void appendRegionContents(HasCoreLocation core, int region,
			ByteBuffer contents) throws StorageException {
		byte[] ary = new byte[contents.remaining()];
		contents.slice().get(ary);
		appendRegionContents(core, region, ary);
	}

	/**
	 * Retrieves some bytes from the database. The bytes represent the contents
	 * of a region of a particular SpiNNaker core.
	 *
	 * @param core
	 *            The core that has the memory region.
	 * @param region
	 *            The region ID.
	 * @return The region contents.
	 * @throws IllegalArgumentException
	 *             If there's no such saved region.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	byte[] getRegionContents(HasCoreLocation core, int region)
			throws StorageException;

	/**
	 * Removes some bytes from the database. The bytes represent the contents of
	 * a region of a particular SpiNNaker core.
	 *
	 * @param core
	 *            The core that has the memory region.
	 * @param region
	 *            The region ID.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	void deleteRegionContents(HasCoreLocation core, int region)
			throws StorageException;

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
}
