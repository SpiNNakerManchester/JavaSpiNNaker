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

/** Executes the host based data specification. */
public class HostExecuteDataSpecification {
	/** Executes the host based data specification. */
	public Map<CoreLocation, LocationInfo> hostExecuteDataSpecification(
			Transceiver txrx, Machine machine, int appID,
			Map<CoreLocation, File> dsgTargets)
			throws IOException, Exception, DataSpecificationException {
		return hostExecuteDataSpecification(txrx, machine, appID, dsgTargets,
				null);
	}

	/** Executes the host based data specification. */
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

	public static class LocationInfo {
		public final int startAddress;
		public final int memoryUsed;
		public final int bytesWrittenBySpec;

		LocationInfo(int startAddress, int memoryUsed, int bytesWrittenBySpec) {
			this.startAddress = startAddress;
			this.memoryUsed = memoryUsed;
			this.bytesWrittenBySpec = bytesWrittenBySpec;
		}
	}
}
