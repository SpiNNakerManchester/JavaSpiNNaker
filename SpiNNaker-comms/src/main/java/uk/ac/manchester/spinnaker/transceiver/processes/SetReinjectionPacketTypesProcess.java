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
import uk.ac.manchester.spinnaker.messages.scp.SetReinjectionPacketTypes;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/** Set the types of packet subject to reinjection. */
public class SetReinjectionPacketTypesProcess
		extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public SetReinjectionPacketTypesProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Set the types of packet to be reinjected in an (extra) monitor.
	 *
	 * @param monitorCore
	 *            the core where the monitor is running.
	 * @param multicast
	 *            If multicast should be set
	 * @param pointToPoint
	 *            If point-to-point should be set
	 * @param fixedRoute
	 *            If fixed-route should be set
	 * @param nearestNeighbour
	 *            If nearest-neighbour should be set
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	public void setPacketTypes(CoreLocation monitorCore, boolean multicast,
			boolean pointToPoint, boolean fixedRoute, boolean nearestNeighbour)
			throws IOException, ProcessException {
		synchronousCall(new SetReinjectionPacketTypes(monitorCore, multicast,
				pointToPoint, fixedRoute, nearestNeighbour));
	}

	/**
	 * Set the types of packet to be reinjected in a set of (extra) monitors.
	 *
	 * @param monitorCoreSubsets
	 *            the cores where the monitors are running.
	 * @param multicast
	 *            If multicast should be set
	 * @param pointToPoint
	 *            If point-to-point should be set
	 * @param fixedRoute
	 *            If fixed-route should be set
	 * @param nearestNeighbour
	 *            If nearest-neighbour should be set
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	public void setPacketTypes(CoreSubsets monitorCoreSubsets,
			boolean multicast, boolean pointToPoint, boolean fixedRoute,
			boolean nearestNeighbour) throws IOException, ProcessException {
		for (ChipLocation chip : monitorCoreSubsets.getChips()) {
			for (Integer p : monitorCoreSubsets.pByChip(chip)) {
				sendRequest(new SetReinjectionPacketTypes(
						new CoreLocation(chip, p), multicast, pointToPoint,
						fixedRoute, nearestNeighbour));
			}
		}
		finish();
		checkForError();
	}
}
