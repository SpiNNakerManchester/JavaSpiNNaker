package uk.ac.manchester.spinnaker.processes;

import static java.util.Collections.unmodifiableList;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_INFO_BYTES;
import static uk.ac.manchester.spinnaker.transceiver.Utils.getVcpuAddress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.messages.model.CPUInfo;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.selectors.ConnectionSelector;

public class GetCPUInfoProcess extends MultiConnectionProcess {
	public GetCPUInfoProcess(ConnectionSelector connectionSelector) {
		super(connectionSelector);
	}

	public List<CPUInfo> getCPUInfo(CoreSubsets coreSubsets)
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
