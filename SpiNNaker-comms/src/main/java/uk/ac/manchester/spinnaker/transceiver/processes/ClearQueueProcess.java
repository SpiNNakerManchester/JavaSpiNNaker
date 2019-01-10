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
import uk.ac.manchester.spinnaker.messages.scp.ClearReinjectionQueue;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/** Clear the packet reinjection queue. */
public class ClearQueueProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public ClearQueueProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Clear the reinjection queues in an (extra) monitor.
	 *
	 * @param monitorCore
	 *            the core where the monitor is running.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	public void clearQueue(CoreLocation monitorCore)
			throws IOException, ProcessException {
		synchronousCall(new ClearReinjectionQueue(monitorCore));
	}

	/**
	 * Clear the reinjection queues in a set of (extra) monitors.
	 *
	 * @param monitorCoreSubsets
	 *            the cores where the monitors are running.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	public void clearQueue(CoreSubsets monitorCoreSubsets)
			throws IOException, ProcessException {
		for (ChipLocation chip : monitorCoreSubsets.getChips()) {
			for (Integer p : monitorCoreSubsets.pByChip(chip)) {
				sendRequest(
						new ClearReinjectionQueue(new CoreLocation(chip, p)));
			}
		}
		finish();
		checkForError();
	}
}
