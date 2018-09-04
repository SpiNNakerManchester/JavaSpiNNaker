package uk.ac.manchester.spinnaker.data_spec;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_MEM_REGIONS;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.processes.Process.Exception;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.utils.progress.ProgressIterable;

/**
 * Executes the host based data specification.
 *
 * @author Donal Fellows
 */
public class HostExecuteDataSpecification {
	/**
	 * Executes the host based data specification.
	 *
	 * @param txrx
	 *            The transceiver used to do the communication.
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
	public Map<CoreLocation, LocationInfo> hostExecuteDataSpecification(
			Transceiver txrx, Machine machine, int appID,
			Map<CoreLocation, File> dsgTargets)
			throws IOException, Exception, DataSpecificationException {
		return hostExecuteDataSpecification(txrx, machine, appID, dsgTargets,
				null);
	}

	/**
	 * Executes the host based data specification.
	 *
	 * @param txrx
	 *            The transceiver used to do the communication.
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
	 * @throws Exception
	 *             if SCAMP rejects a message
	 * @throws DataSpecificationException
	 *             if there's a bug in a data spec file
	 */
	public Map<CoreLocation, LocationInfo> hostExecuteDataSpecification(
			Transceiver txrx, Machine machine, int appID,
			Map<CoreLocation, File> dsgTargets,
			Map<CoreLocation, LocationInfo> info)
			throws IOException, Exception, DataSpecificationException {
		if (info == null) {
			info = new HashMap<>();
		}
		for (CoreLocation core : new ProgressIterable<>(dsgTargets.keySet(),
				"Executing data specifications and loading data")) {
			info.put(core, executeDSGFirstTime(txrx, machine, appID, core,
					dsgTargets.get(core)));
		}
		return info;
	}

	/**
	 * Executes the host based data specification on a specific core.
	 *
	 * @param txrx
	 *            The transceiver used to do the communication.
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
	protected LocationInfo executeDSGFirstTime(Transceiver txrx,
			Machine machine, int appID, HasCoreLocation core, File dataSpec)
			throws IOException, Exception, DataSpecificationException {
		try (Executor executor =
				new Executor(dataSpec, machine.getChipAt(core).sdram)) {
			executor.execute();
			int bytesUsed = executor.getConstructedDataSize();
			int startAddress = txrx.mallocSDRAM(core, bytesUsed, appID);

			ByteBuffer b = allocate(bytesUsed).order(LITTLE_ENDIAN);
			executor.addHeader(b);
			executor.addPointerTable(b, startAddress);
			b.flip();
			txrx.writeMemory(core, startAddress, b);
			int bytesWritten = b.remaining();

			for (int rID = 0; rID < MAX_MEM_REGIONS; rID++) {
				MemoryRegion r = executor.getRegion(rID);
				if (r == null || r.isUnfilled()
						|| r.getMaxWritePointer() <= 0) {
					continue;
				}

				ByteBuffer data = r.getRegionData().duplicate();
				data.flip();
				txrx.writeMemory(core, r.getRegionBase(), data);
				bytesWritten += data.remaining();
			}

			int user0 = txrx.getUser0RegisterAddress(core);
			txrx.writeMemory(core, user0, startAddress);
			return new LocationInfo(startAddress, bytesUsed, bytesWritten);
		}

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
