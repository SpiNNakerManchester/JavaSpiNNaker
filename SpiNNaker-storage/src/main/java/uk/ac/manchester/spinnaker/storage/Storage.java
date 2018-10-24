package uk.ac.manchester.spinnaker.storage;

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
	 * Get a list of all cores known to the database. <i>Warning: this is a
	 * potentially expensive operation!</i>
	 *
	 * @return A list of known cores.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	List<CoreLocation> getCores() throws StorageException;

	/**
	 * Get a list of all cores known to the database that have stored data.
	 * <i>Warning: this is a potentially expensive operation!</i>
	 *
	 * @return A list of known cores with stored region data.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	List<CoreLocation> getCoresWithData() throws StorageException;

	/**
	 * Get a list of all known regions for a particular core.
	 *
	 * @param core
	 *            The core that has the memory regions.
	 * @return A list of region IDs for which either something is stored or for
	 *         which we know where in memory they are mapped.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	List<Integer> getRegions(HasCoreLocation core) throws StorageException;

	/**
	 * Get a list of all known regions with stored data for a particular core.
	 *
	 * @param core
	 *            The core that has the memory regions.
	 * @return A list of region IDs for which something is stored.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	List<Integer> getRegionsWithData(HasCoreLocation core)
			throws StorageException;

	/**
	 * Store in the database the fact that a core has the given defined regions.
	 *
	 * @param core
	 *            The core that has the memory regions.
	 * @param regions
	 *            The description <i>in order</i> of those memory regions that
	 *            the core has.
	 */
	void rememberLocations(CoreLocation core, List<RegionDescriptor> regions)
			throws StorageException;

	/**
	 * Fetch the location of a region on a core.
	 *
	 * @param core
	 *            The core that has the memory regions.
	 * @param region
	 *            The ID region to get the location of.
	 * @return Where the region is and what size it is, or {@code null} if there
	 *         is no information for such a region.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	RegionDescriptor getRegionLocation(CoreLocation core, int region)
			throws StorageException;
}
