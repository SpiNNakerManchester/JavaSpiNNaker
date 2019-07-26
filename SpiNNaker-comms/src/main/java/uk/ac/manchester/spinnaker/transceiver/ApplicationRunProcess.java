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

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.scp.ApplicationRun;

/** Launch an application. */
class ApplicationRunProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * Create.
	 *
	 * @param connectionSelector
	 *            How to choose where to send messages.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public ApplicationRunProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Launch an application (already loaded).
	 *
	 * @param appID
	 *            The application ID to launch.
	 * @param coreSubsets
	 *            Which cores to launch.
	 * @param wait
	 *            Whether to wait for the application launch to fully complete
	 *            before returning.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public void run(AppID appID, CoreSubsets coreSubsets, boolean wait)
			throws ProcessException, IOException {
		for (ChipLocation chip : coreSubsets.getChips()) {
			sendRequest(new ApplicationRun(appID, chip,
					coreSubsets.pByChip(chip), wait));
		}
		finish();
		checkForError();
	}
}
