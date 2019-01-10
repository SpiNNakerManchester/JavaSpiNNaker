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
import uk.ac.manchester.spinnaker.messages.scp.ResetReinjectionCounters;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/** Reset the packet reinjection counters. */
public class ResetReinjectionCountersProcess
		extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public ResetReinjectionCountersProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Reset the reinjection counters in an (extra) monitor.
	 *
	 * @param monitorCore
	 *            the core where the monitor is running.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	public void resetCounters(CoreLocation monitorCore)
			throws IOException, ProcessException {
		synchronousCall(new ResetReinjectionCounters(monitorCore));
	}

	/**
	 * Reset the reinjection counters in a set of (extra) monitors.
	 *
	 * @param coreSubsets
	 *            the cores where the monitors are running.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	public void resetCounters(CoreSubsets coreSubsets)
			throws IOException, ProcessException {
		for (ChipLocation chip : coreSubsets.getChips()) {
			for (Integer p : coreSubsets.pByChip(chip)) {
				sendRequest(new ResetReinjectionCounters(
						new CoreLocation(chip, p)));
			}
		}
		finish();
		checkForError();
	}
}
