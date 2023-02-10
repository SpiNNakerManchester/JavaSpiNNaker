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

import static uk.ac.manchester.spinnaker.messages.scp.RunningCommand.NEW_RUNTIME_ID;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP Request to update the runtime info on a core. There is no response
 * payload.
 * <p>
 * This calls {@code simulation_control_scp_callback()} in {@code simulation.c}.
 */
public final class UpdateRuntime extends FECRequest<EmptyResponse> {
	/**
	 * @param core
	 *            The SpiNNaker core to update the runtime info of.
	 * @param runTime
	 *            The number of machine timesteps.
	 * @param infiniteRun
	 *            Whether we are doing infinite running.
	 * @param currentTime
	 *            The current simulation time.
	 * @param numSyncSteps
	 *            The number of timesteps before we pause to synchronise.
	 */
	public UpdateRuntime(HasCoreLocation core, int runTime, boolean infiniteRun,
			int currentTime, int numSyncSteps) {
		super(core, true, NEW_RUNTIME_ID, runTime, bool(infiniteRun),
				currentTime, numSyncSteps);
	}

	private static int bool(boolean value) {
		return value ? 1 : 0;
	}

	@Override
	public EmptyResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new EmptyResponse("update runtime", NEW_RUNTIME_ID, buffer);
	}
}
