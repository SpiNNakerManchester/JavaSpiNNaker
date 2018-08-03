package uk.ac.manchester.spinnaker.processes;

import static java.util.Collections.unmodifiableList;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_INFO_BYTES;
import static uk.ac.manchester.spinnaker.transceiver.Utils.getVcpuAddress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.messages.model.CPUInfo;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;

/**
 * Get the CPU information structure for a set of processors.
 */
public class GetCPUInfoProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 */
	public GetCPUInfoProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
	}

	/**
	 * Get CPU information.
	 *
	 * @param coreSubsets
	 *            What processors to get the information from
	 * @return The CPU information, in undetermined order.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public Collection<CPUInfo> getCPUInfo(CoreSubsets coreSubsets)
			throws IOException, Exception {
		List<CPUInfo> cpuInfo = new ArrayList<>();
		for (CoreLocation core : coreSubsets) {
			sendRequest(
					new ReadMemory(core.getScampCore(), getVcpuAddress(core),
							CPU_INFO_BYTES),
					response -> cpuInfo.add(new CPUInfo(core, response.data)));
		}
		finish();
		checkForError();
		return unmodifiableList(cpuInfo);
	}
}
