package uk.ac.manchester.spinnaker.data_spec;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_HEADER_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.INT_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_MEM_REGIONS;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;
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
	 * @throws ProcessException
	 *             if SCAMP rejects a message
	 * @throws DataSpecificationException
	 *             if there's a bug in a data spec file
	 */
	public Map<CoreLocation, LocationInfo> load(Machine machine, int appID,
			Map<CoreLocation, File> dsgTargets)
			throws IOException, ProcessException, DataSpecificationException {
		return load(machine, appID, dsgTargets, null);
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
	 *            happened on what core.
	 * @return Information about what was written on each core where a write
	 *         happened.
	 * @throws IOException
	 *             if communication or disk IO fail
	 * @throws ProcessException
	 *             if SCAMP rejects a message
	 * @throws DataSpecificationException
	 *             if there's a bug in a data spec file
	 */
	public Map<CoreLocation, LocationInfo> load(Machine machine, int appID,
			Map<CoreLocation, File> dsgTargets,
			Map<CoreLocation, LocationInfo> info)
			throws IOException, ProcessException, DataSpecificationException {
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
	 * @throws ProcessException
	 *             if SCAMP rejects a message
	 * @throws DataSpecificationException
	 *             if there's a bug in a data spec file
	 */
	public LocationInfo load(Machine machine, int appID, HasCoreLocation core,
			File dataSpec)
			throws IOException, ProcessException, DataSpecificationException {
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
			return new LocationInfo(startAddress, bytesUsed, bytesWritten);
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
	 * @throws ProcessException
	 *             If SCAMP rejects the request.
	 */
	private int writeHeader(HasCoreLocation core, Executor executor,
			int startAddress) throws IOException, ProcessException {
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
	 * @throws ProcessException
	 *             If SCAMP rejects the request.
	 */
	private int writeRegion(HasCoreLocation core, MemoryRegion region)
			throws IOException, ProcessException {
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
	 * @throws ProcessException
	 *             If SCAMP rejects the request.
	 */
	private int writeRegion(HasCoreLocation core, MemoryRegion region,
			int baseAddress) throws IOException, ProcessException {
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
	 * @throws ProcessException
	 *             if SCAMP rejects a message
	 * @throws DataSpecificationException
	 *             if there's a bug in a data spec file
	 */
	public void reload(HasCoreLocation core, File dataSpec)
			throws DataSpecificationException, IOException, ProcessException {
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
	 * @throws ProcessException
	 *             if SCAMP rejects a message
	 * @throws DataSpecificationException
	 *             if there's a bug in a data spec file
	 */
	public void reload(HasCoreLocation core, LocationInfo info, File dataSpec)
			throws DataSpecificationException, IOException, ProcessException {
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

		LocationInfo(int startAddress, int memoryUsed, int bytesWrittenBySpec) {
			this.startAddress = startAddress;
			this.memoryUsed = memoryUsed;
			this.bytesWrittenBySpec = bytesWrittenBySpec;
		}
	}
}
