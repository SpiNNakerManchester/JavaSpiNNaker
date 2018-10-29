package uk.ac.manchester.spinnaker.front_end.data_spec;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_HEADER_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.INT_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_MEM_REGIONS;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.manchester.spinnaker.data_spec.Executor;
import uk.ac.manchester.spinnaker.data_spec.MemoryRegion;
import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.storage.DatabaseEngine;
import uk.ac.manchester.spinnaker.storage.RegionDescriptor;
import uk.ac.manchester.spinnaker.storage.SQLiteStorage;
import uk.ac.manchester.spinnaker.storage.Storage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.Process.Exception;
import uk.ac.manchester.spinnaker.utils.progress.ProgressIterable;

/**
 * Executes the host based data specification.
 *
 * @author Donal Fellows
 */
public class HostExecuteDataSpecification {
	private static final int DEFAULT_SDRAM_BYTES = 117 * 1024 * 1024;
	private static final int REGION_TABLE_SIZE = MAX_MEM_REGIONS * INT_SIZE;
	private Transceiver txrx;

	/**
	 * Create a high-level DSE interface.
	 *
	 * @param transceiver
	 *            The transceiver used to do the communication.
	 */
	public HostExecuteDataSpecification(Transceiver transceiver) {
		this.txrx = transceiver;
	}

	/**
	 * Executes the host based data specification.
	 *
	 * @param machine
	 *            The machine we're talking to.
	 * @param appID
	 *            The application we're loading.
	 * @param dsgTargets
	 *            Where to find a file for each core to write that holds the
	 *            data specification for that core.
	 * @return Information about what was written on each core where a write
	 *         happened.
	 * @throws IOException
	 *             if communication or disk IO fail
	 * @throws Exception
	 *             if SCAMP rejects a message
	 * @throws DataSpecificationException
	 *             if there's a bug in a data spec file
	 */
	public Map<CoreLocation, LocationInfo> load(Machine machine, int appID,
			Map<CoreLocation, File> dsgTargets)
			throws IOException, Exception, DataSpecificationException {
		Map<CoreLocation, LocationInfo> info = new HashMap<>();
		return load(machine, appID, dsgTargets, info);
	}

	/**
	 * Executes the host based data specification.
	 *
	 * @param machine
	 *            The machine we're talking to.
	 * @param appID
	 *            The application we're loading.
	 * @param dsgTargets
	 *            Where to find a file for each core to write that holds the
	 *            data specification for that core.
	 * @param info
	 *            The map to update with information about what writes have
	 *            happened on what core. {@code null} to allocate a new map.
	 * @return Information about what was written on each core where a write
	 *         happened.
	 * @throws IOException
	 *             if communication or disk IO fail
	 * @throws Exception
	 *             if SCAMP rejects a message
	 * @throws DataSpecificationException
	 *             if there's a bug in a data spec file
	 */
	public Map<CoreLocation, LocationInfo> load(Machine machine, int appID,
			Map<CoreLocation, File> dsgTargets,
			Map<CoreLocation, LocationInfo> info)
			throws IOException, Exception, DataSpecificationException {
		if (info == null) {
			info = new HashMap<>();
		}
		for (CoreLocation core : new ProgressIterable<>(dsgTargets.keySet(),
				"Executing data specifications and loading data")) {
			info.put(core, load(machine, appID, core, dsgTargets.get(core)));
		}
		return info;
	}

	/**
	 * Executes the host based data specification.
	 *
	 * @param machine
	 *            The machine we're talking to.
	 * @param appID
	 *            The application we're loading.
	 * @param dsgTargets
	 *            Where to find a file for each core to write that holds the
	 *            data specification for that core.
	 * @param storage
	 *            Where to record information about the layout of the regions on
	 *            the core.
	 * @throws IOException
	 *             if communication or disk IO fail
	 * @throws Exception
	 *             if SCAMP rejects a message
	 * @throws DataSpecificationException
	 *             if there's a bug in a data spec file
	 * @throws StorageException
	 *             If the database rejects the information.
	 */
	public void load(Machine machine, int appID,
			Map<CoreLocation, File> dsgTargets, Storage storage)
			throws IOException, Exception, DataSpecificationException,
			StorageException {
		for (CoreLocation core : new ProgressIterable<>(dsgTargets.keySet(),
				"Executing data specifications and loading data")) {
			LocationInfo info =
					load(machine, appID, core, dsgTargets.get(core));
			storage.rememberLocations(core, info.memoryRegions);
		}
	}

	/**
	 * Executes the host based data specification on a specific core.
	 *
	 * @param machine
	 *            The machine we're talking to.
	 * @param appID
	 *            The application we're loading.
	 * @param core
	 *            Which core of the machine to generate for and load onto.
	 * @param dataSpec
	 *            The file holding the data specification for the core.
	 * @return Information about what was written.
	 * @throws IOException
	 *             if communication or disk IO fail
	 * @throws Exception
	 *             if SCAMP rejects a message
	 * @throws DataSpecificationException
	 *             if there's a bug in a data spec file
	 */
	public LocationInfo load(Machine machine, int appID, HasCoreLocation core,
			File dataSpec)
			throws IOException, Exception, DataSpecificationException {
		try (Executor executor =
				new Executor(dataSpec, machine.getChipAt(core).sdram)) {
			executor.execute();
			int bytesUsed = executor.getConstructedDataSize();
			int startAddress = txrx.mallocSDRAM(core, bytesUsed, appID);
			int bytesWritten = writeHeader(core, executor, startAddress);

			for (MemoryRegion r : executor.regions()) {
				if (!isToBeIgnored(r)) {
					bytesWritten += writeRegion(core, r);
				}
			}

			int user0 = txrx.getUser0RegisterAddress(core);
			txrx.writeMemory(core, user0, startAddress);
			return new LocationInfo(startAddress, bytesUsed, bytesWritten,
					executor.regions());
		}
	}

	/**
	 * Writes the header section.
	 *
	 * @param core
	 *            Which core to write to.
	 * @param executor
	 *            The executor which generates the header.
	 * @param startAddress
	 *            Where to write the header.
	 * @return How many bytes were actually written.
	 * @throws IOException
	 *             If anything goes wrong with I/O.
	 * @throws Exception
	 *             If SCAMP rejects the request.
	 */
	private int writeHeader(HasCoreLocation core, Executor executor,
			int startAddress) throws IOException, Exception {
		ByteBuffer b = allocate(APP_PTR_TABLE_HEADER_SIZE + REGION_TABLE_SIZE)
				.order(LITTLE_ENDIAN);

		executor.addHeader(b);
		executor.addPointerTable(b, startAddress);

		b.flip();
		int written = b.remaining();
		txrx.writeMemory(core, startAddress, b);
		return written;
	}

	/**
	 * Writes the contents of a region. Caller is responsible for ensuring this
	 * method has work to do.
	 *
	 * @param core
	 *            Which core to write to.
	 * @param region
	 *            The region to write.
	 * @return How many bytes were actually written.
	 * @throws IOException
	 *             If anything goes wrong with I/O.
	 * @throws Exception
	 *             If SCAMP rejects the request.
	 */
	private int writeRegion(HasCoreLocation core, MemoryRegion region)
			throws IOException, Exception {
		return writeRegion(core, region, region.getRegionBase());
	}

	/**
	 * Writes the contents of a region. Caller is responsible for ensuring this
	 * method has work to do.
	 *
	 * @param core
	 *            Which core to write to.
	 * @param region
	 *            The region to write.
	 * @param baseAddress
	 *            Where to write the region.
	 * @return How many bytes were actually written.
	 * @throws IOException
	 *             If anything goes wrong with I/O.
	 * @throws Exception
	 *             If SCAMP rejects the request.
	 */
	private int writeRegion(HasCoreLocation core, MemoryRegion region,
			int baseAddress) throws IOException, Exception {
		ByteBuffer data = region.getRegionData().duplicate();

		data.flip();
		int written = data.remaining();
		txrx.writeMemory(core, baseAddress, data);
		return written;
	}

	/**
	 * Executes the host based data specification on a specific core for the
	 * purpose of reloading, using default assumptions about how much space was
	 * allocated.
	 *
	 * @param core
	 *            Which core of the machine to generate for and load onto.
	 * @param dataSpec
	 *            The file holding the data specification for the core.
	 * @throws IOException
	 *             if communication or disk IO fail
	 * @throws Exception
	 *             if SCAMP rejects a message
	 * @throws DataSpecificationException
	 *             if there's a bug in a data spec file
	 */
	public void reload(HasCoreLocation core, File dataSpec)
			throws DataSpecificationException, IOException, Exception {
		try (Executor executor = new Executor(dataSpec, DEFAULT_SDRAM_BYTES)) {
			// execute the spec
			executor.execute();

			// Read the region table for the placement
			int baseAddress = txrx.getCPUInformation(core).getUser(0);
			IntBuffer offsets = txrx.readMemory(core.asChipLocation(),
					baseAddress + APP_PTR_TABLE_HEADER_SIZE, REGION_TABLE_SIZE)
					.asIntBuffer();

			// Write the regions to the machine
			for (MemoryRegion r : executor.regions()) {
				int offset = offsets.get();
				if (!isToBeIgnored(r)) {
					writeRegion(core, r, offset);
				}
			}
		}
	}

	/**
	 * Executes the host based data specification on a specific core for the
	 * purpose of reloading.
	 *
	 * @param core
	 *            Which core of the machine to generate for and load onto.
	 * @param info
	 *            Description of what was previously actually allocated by this
	 *            core's previous DSE execution.
	 * @param dataSpec
	 *            The file holding the data specification for the core.
	 * @throws IOException
	 *             if communication or disk IO fail
	 * @throws Exception
	 *             if SCAMP rejects a message
	 * @throws DataSpecificationException
	 *             if there's a bug in a data spec file
	 */
	public void reload(HasCoreLocation core, LocationInfo info, File dataSpec)
			throws DataSpecificationException, IOException, Exception {
		try (Executor executor = new Executor(dataSpec, info.memoryUsed)) {
			// execute the spec
			executor.execute();

			// Read the region table for the placement
			int tableAddress = txrx.getCPUInformation(core).getUser(0);
			IntBuffer offsets = txrx.readMemory(core.asChipLocation(),
					tableAddress + APP_PTR_TABLE_HEADER_SIZE, REGION_TABLE_SIZE)
					.asIntBuffer();

			// Write the regions to the machine
			for (MemoryRegion r : executor.regions()) {
				int offset = offsets.get();
				if (!isToBeIgnored(r)) {
					writeRegion(core, r, offset);
				}
			}
		}
	}

	private static boolean isToBeIgnored(MemoryRegion r) {
		return r == null || r.isUnfilled() || r.getMaxWritePointer() <= 0;
	}

	private static final String FNRE = "^(\\d+)_(\\d+)_(\\d+)\\.spec$";

	public static void main(String... args)
			throws IOException, SpinnmanException, Exception,
			DataSpecificationException, StorageException {
		InetAddress host = InetAddress.getByName(args[0]);
		int version = Integer.parseInt(args[1]);
		int appID = Integer.parseInt(args[2]);
		File db = new File(args[3]);
		File dsgDir = new File(args[4]);

		Map<CoreLocation, File> dsgTargets = new HashMap<>();
		Pattern pat = Pattern.compile(FNRE);
		for (File f : dsgDir.listFiles((f, n) -> n.matches(FNRE))) {
			Matcher m = pat.matcher(f.getName());
			if (m.matches()) {
				int x = Integer.parseInt(m.group(1));
				int y = Integer.parseInt(m.group(2));
				int p = Integer.parseInt(m.group(3));
				dsgTargets.put(new CoreLocation(x, y, p), f);
			}
		}

		Storage storage = new SQLiteStorage(new DatabaseEngine(db));
		Transceiver t = new Transceiver(host, version);
		HostExecuteDataSpecification dse = new HostExecuteDataSpecification(t);
		dse.load(t.getMachineDetails(), appID, dsgTargets, storage);
	}

	/**
	 * A description of information about what writes were done.
	 *
	 * @author Donal Fellows
	 */
	public static class LocationInfo {
		/** Where did the writes start. */
		public final int startAddress;
		/** How much memory was allocated. */
		public final int memoryUsed;
		/** How much memory was written. No more than the space allocated. */
		public final int bytesWrittenBySpec;
		/** Where the writes were really done. */
		public final List<RegionDescriptor> memoryRegions;

		LocationInfo(int startAddress, int memoryUsed, int bytesWrittenBySpec,
				Collection<MemoryRegion> regions) {
			this.startAddress = startAddress;
			this.memoryUsed = memoryUsed;
			this.bytesWrittenBySpec = bytesWrittenBySpec;
			this.memoryRegions = unmodifiableList(regions.stream()
					.map(r -> new RegionDescriptor(r.getRegionBase(),
							r.getAllocatedSize()))
					.collect(toList()));
		}
	}
}
