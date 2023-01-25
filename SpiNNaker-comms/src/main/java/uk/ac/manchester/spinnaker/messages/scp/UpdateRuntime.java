/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.RunningCommand.NEW_RUNTIME_ID;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP Request to update the runtime info on a core.
 * <p>
 * This calls {@code simulation_control_scp_callback()} in {@code simulation.c}.
 */
public class UpdateRuntime extends FECRequest<CheckOKResponse> {
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
	public UpdateRuntime(HasCoreLocation core, int runTime,
			boolean infiniteRun, int currentTime, int numSyncSteps) {
		super(core, true, NEW_RUNTIME_ID, runTime, bool(infiniteRun),
				currentTime, numSyncSteps);
	}

	private static int bool(boolean value) {
		return value ? 1 : 0;
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new CheckOKResponse("update runtime", NEW_RUNTIME_ID, buffer);
	}
}
