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
	private final HasCoreLocation originatingPlacement;

	/**
	 * Create an instance of the protocol implementation.
	 *
	 * @param storage
	 *            The database to write the data to.
	 * @param extraMonitorPlacement
	 *            What core on SpiNNaker do we download from.
	 * @param originatingPlacement
	 *            What core on SpiNNaker generated the data.
	 * @param region
	 *            What memory region of the core we should download.
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
			HasCoreLocation extraMonitorPlacement,
			HasCoreLocation originatingPlacement, int region, String hostname,
			HasChipLocation chip, int iptag)
			throws UnknownHostException, StorageException {
		this(storage, extraMonitorPlacement, originatingPlacement, region,
				requireNonNull(
						storage.getRegionLocation(extraMonitorPlacement,
								region),
						"region location and size are unknown"),
				hostname, chip, iptag);
	}

	@SuppressWarnings("checkstyle:ParameterNumber")
	private StorageBackedHostDataReceiver(Storage storage,
			HasCoreLocation extraMonitorPlacement,
			HasCoreLocation originatingPlacement, int region,
			RegionDescriptor d, String hostname, HasChipLocation ethernetChip,
			int iptag) throws UnknownHostException, StorageException {
		super(extraMonitorPlacement, hostname, d.size, d.baseAddress,
				ethernetChip, iptag);
		if (!originatingPlacement.onSameChipAs(extraMonitorPlacement)) {
			throw new IllegalArgumentException(
					"placements for extra monitor core ("
							+ extraMonitorPlacement.asCoreLocation()
							+ ") and data producing core ("
							+ originatingPlacement.asCoreLocation()
							+ ") must be on the same chip");
		}
		this.storage = storage;
		this.region = region;
		this.originatingPlacement = originatingPlacement;
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
		storage.storeRegionContents(originatingPlacement, region, getData());
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
		storage.appendRegionContents(originatingPlacement, region, getData());
	}
}
