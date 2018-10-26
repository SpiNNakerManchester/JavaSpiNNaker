package uk.ac.manchester.spinnaker.front_end.download;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.UnknownHostException;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.storage.RegionDescriptor;
import uk.ac.manchester.spinnaker.storage.Storage;
import uk.ac.manchester.spinnaker.storage.StorageException;

/**
 * An implementation of the SpiNNaker Fast Data Download Protocol that
 * integrates with the storage system.
 *
 * @author Donal Fellows
 */
public class StorageBackedHostDataReceiver extends HostDataReceiver {
	private final Storage storage;
	private final int region;
	private final HasCoreLocation placement;

	/**
	 * Create an instance of the protocol implementation.
	 *
	 * @param storage
	 *            The database to write the data to.
	 * @param placement
	 *            What core on SpiNNaker do we download from.
	 * @param region
	 *            What memory region of the core we should download.
	 * @param portConnection
	 *            What port we talk to.
	 * @param hostname
	 *            Where the SpiNNaker machine is.
	 * @param chip
	 *            Used for reprogramming the IP Tag.
	 * @param iptag
	 *            The ID of the IP Tag.
	 * @throws UnknownHostException
	 *             If the hostname can't be resolved to an IPv4 address.
	 * @throws StorageException
	 *             If the database access fails.
	 */
	public StorageBackedHostDataReceiver(Storage storage,
			HasCoreLocation placement, int region, int portConnection,
			String hostname, HasChipLocation chip, int iptag)
			throws UnknownHostException, StorageException {
		this(storage, placement, region,
				requireNonNull(storage.getRegionLocation(placement, region),
						"region location and size are unknown"),
				portConnection, hostname, chip, iptag);
	}

	@SuppressWarnings("checkstyle:ParameterNumber")
	private StorageBackedHostDataReceiver(Storage storage,
			HasCoreLocation placement, int region, RegionDescriptor d,
			int portConnection, String hostname, HasChipLocation chip,
			int iptag) throws UnknownHostException, StorageException {
		super(portConnection, placement, hostname, d.size, d.baseAddress, chip,
				iptag);
		this.storage = storage;
		this.region = region;
		this.placement = placement;
	}

	/**
	 * Write the contents of the registered region to storage, replacing what is
	 * already there if it exists.
	 *
	 * @throws IOException
	 *             If the I/O operations on the file fail or if the actual
	 *             reading of the data fails.
	 * @throws InterruptedException
	 *             If the process of doing the download is interrupted.
	 * @throws StorageException
	 *             If the database access fails.
	 */
	public void writeData()
			throws InterruptedException, IOException, StorageException {
		storage.storeRegionContents(placement, region, getData());
	}

	/**
	 * Append the contents of the registered region to storage.
	 *
	 * @throws IOException
	 *             If the I/O operations on the file fail or if the actual
	 *             reading of the data fails.
	 * @throws InterruptedException
	 *             If the process of doing the download is interrupted.
	 * @throws StorageException
	 *             If the database access fails.
	 */
	public void appendData()
			throws InterruptedException, IOException, StorageException {
		storage.appendRegionContents(placement, region, getData());
	}
}
