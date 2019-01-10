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

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.messages.scp.SetRouterEmergencyTimeout;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/** Set the emergency timeouts for a set of routers. */
public class SetRouterEmergencyTimeoutProcess
		extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public SetRouterEmergencyTimeoutProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Set the emergency timeout in an (extra) monitor's router.
	 *
	 * @param monitorCore
	 *            the core where the monitor is running.
	 * @param timeoutMantissa
	 *            The mantissa of the timeout value, between 0 and 15.
	 * @param timeoutExponent
	 *            The exponent of the timeout value, between 0 and 15.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	public void setTimeout(CoreLocation monitorCore, int timeoutMantissa,
			int timeoutExponent) throws IOException, ProcessException {
		synchronousCall(new SetRouterEmergencyTimeout(monitorCore,
				timeoutMantissa, timeoutExponent));
	}

	/**
	 * Set the emergency timeout in a set of (extra) monitors' routers.
	 *
	 * @param monitorCoreSubsets
	 *            the cores where the monitors are running.
	 * @param timeoutMantissa
	 *            The mantissa of the timeout value, between 0 and 15.
	 * @param timeoutExponent
	 *            The exponent of the timeout value, between 0 and 15.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	public void setTimeout(CoreSubsets monitorCoreSubsets,
			int timeoutMantissa, int timeoutExponent)
			throws IOException, ProcessException {
		for (ChipLocation chip : monitorCoreSubsets.getChips()) {
			for (Integer p : monitorCoreSubsets.pByChip(chip)) {
				sendRequest(
						new SetRouterEmergencyTimeout(new CoreLocation(chip, p),
								timeoutMantissa, timeoutExponent));
			}
		}
		finish();
		checkForError();
	}
}
