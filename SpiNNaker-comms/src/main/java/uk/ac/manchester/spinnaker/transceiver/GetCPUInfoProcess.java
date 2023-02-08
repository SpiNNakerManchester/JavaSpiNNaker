/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.transceiver;

import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_INFO_BYTES;
import static uk.ac.manchester.spinnaker.transceiver.Utils.getVcpuAddress;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.messages.model.CPUInfo;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * Get the CPU information structure for a set of processors.
 */
class GetCPUInfoProcess extends TxrxProcess {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	GetCPUInfoProcess(
			ConnectionSelector<? extends SCPConnection> connectionSelector,
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	MappableIterable<CPUInfo> getCPUInfo(CoreSubsets coreSubsets)
			throws IOException, ProcessException, InterruptedException {
		var cpuInfo = new ArrayList<CPUInfo>();
		for (var core : requireNonNull(coreSubsets,
				"must have actual core subset to iterate over")) {
			sendGet(new ReadMemory(core.getScampCore(), getVcpuAddress(core),
					CPU_INFO_BYTES),
					bytes -> cpuInfo.add(new CPUInfo(core, bytes)));
		}
		finishBatch();
		return cpuInfo::iterator;
	}
}
