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
package uk.ac.manchester.spinnaker.transceiver.processes;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.messages.scp.UpdateRuntime;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/** Update the running time configuration on some cores. */
public class UpdateRuntimeProcess
		extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public UpdateRuntimeProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Update the running time configuration of some cores.
	 *
	 * @param runTimesteps
	 *            The number of machine timesteps to run for. {@code null}
	 *            indicates an infinite run.
	 * @param coreSubsets
	 *            the cores to update the information of.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	public void updateRuntime(Integer runTimesteps, CoreSubsets coreSubsets)
			throws IOException, ProcessException {
		int runTime = (runTimesteps == null ? 0 : runTimesteps);
		boolean infiniteRun = runTimesteps == null;
		for (CoreLocation core : requireNonNull(coreSubsets,
				"must have actual core subset to iterate over")) {
			sendRequest(new UpdateRuntime(core, runTime, infiniteRun, true));
		}
		finish();
		checkForError();
	}
}
