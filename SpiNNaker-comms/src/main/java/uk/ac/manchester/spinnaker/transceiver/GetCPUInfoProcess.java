/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.transceiver;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_INFO_BYTES;
import static uk.ac.manchester.spinnaker.transceiver.Utils.getVcpuAddress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.messages.model.CPUInfo;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;

/**
 * Get the CPU information structure for a set of processors.
 */
class GetCPUInfoProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	GetCPUInfoProcess(ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Get CPU information.
	 *
	 * @param coreSubsets
	 *            What processors to get the information from
	 * @return The CPU information, in undetermined order.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	Collection<CPUInfo> getCPUInfo(CoreSubsets coreSubsets)
			throws IOException, ProcessException {
		var cpuInfo = new ArrayList<CPUInfo>();
		for (var core : requireNonNull(coreSubsets,
				"must have actual core subset to iterate over")) {
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
