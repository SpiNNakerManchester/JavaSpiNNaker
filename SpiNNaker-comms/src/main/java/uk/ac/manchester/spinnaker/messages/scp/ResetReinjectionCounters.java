/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.ReinjectorCommand.RESET_COUNTERS;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * An SCP Request to reset the statistics counters of the dropped packet
 * reinjection. There is no response payload.
 * <p>
 * Handled by {@code reinjection_reset_counters()} in
 * {@code extra_monitor_support.c}.
 */
public class ResetReinjectionCounters
		extends ReinjectorRequest<CheckOKResponse> {
	/**
	 * @param core
	 *            The coordinates of the monitor core.
	 */
	public ResetReinjectionCounters(HasCoreLocation core) {
		super(core, RESET_COUNTERS);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Reset dropped packet reinjection counters",
				RESET_COUNTERS, buffer);
	}
}
