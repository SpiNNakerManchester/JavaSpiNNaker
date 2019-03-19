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

import static java.util.Collections.unmodifiableMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus;
import uk.ac.manchester.spinnaker.messages.scp.GetReinjectionStatus;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/**
 * Get the reinjection status from a set of monitor cores. Each monitor core
 * must be running the correct binary.
 */
public class ReadReinjectionStatusProcess
		extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public ReadReinjectionStatusProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Get reinjection status from a single monitor.
	 *
	 * @param monitorCore
	 *            What core is running the monitor.
	 * @return The reinjection status.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public ReinjectionStatus getReinjectionStatus(CoreLocation monitorCore)
			throws IOException, ProcessException {
		return synchronousCall(
				new GetReinjectionStatus(monitorCore)).reinjectionStatus;
	}

	/**
	 * Get the reinjection status from a collection of monitors.
	 *
	 * @param monitorCoreSubsets
	 *            What processors to get the information from
	 * @return The map from cores to their reinjection statuses.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public Map<CoreLocation, ReinjectionStatus> getReinjectionStatus(
			CoreSubsets monitorCoreSubsets)
			throws IOException, ProcessException {
		Map<CoreLocation, ReinjectionStatus> status = new HashMap<>();
		for (CoreLocation core : monitorCoreSubsets) {
			sendRequest(new GetReinjectionStatus(core),
					response -> status.put(core, response.reinjectionStatus));
		}
		finish();
		checkForError();
		return unmodifiableMap(status);
	}
}
