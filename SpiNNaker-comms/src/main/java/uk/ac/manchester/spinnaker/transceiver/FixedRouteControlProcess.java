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

import static uk.ac.manchester.spinnaker.messages.model.AppID.DEFAULT;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.RoutingEntry;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.scp.FixedRouteInitialise;
import uk.ac.manchester.spinnaker.messages.scp.FixedRouteRead;

/** Load a fixed route routing entry onto a chip, and read it back again. */
class FixedRouteControlProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	FixedRouteControlProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Load a fixed route routing entry onto a chip with a default application
	 * ID.
	 *
	 * @param chip
	 *            The coordinates of the chip.
	 * @param fixedRoute
	 *            the fixed route entry
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	void loadFixedRoute(HasChipLocation chip, RoutingEntry fixedRoute)
			throws IOException, ProcessException {
		loadFixedRoute(chip, fixedRoute, DEFAULT);
	}

	/**
	 * Load a fixed route routing entry onto a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip.
	 * @param fixedRoute
	 *            the fixed route entry
	 * @param appID
	 *            The ID of the application with which to associate the routes.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	void loadFixedRoute(HasChipLocation chip, RoutingEntry fixedRoute,
			AppID appID) throws IOException, ProcessException {
		int entry = fixedRoute.encode();
		synchronousCall(new FixedRouteInitialise(chip, entry, appID));
	}

	/**
	 * Read the current fixed route from a chip.
	 *
	 * @param chip
	 *            The chip to read from
	 * @return The route.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	RoutingEntry readFixedRoute(HasChipLocation chip)
			throws IOException, ProcessException {
		return readFixedRoute(chip, DEFAULT);
	}

	/**
	 * Read the current fixed route from a chip.
	 *
	 * @param chip
	 *            The chip to read from
	 * @param appID
	 *            The application ID associated with the route.
	 * @return The route.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	RoutingEntry readFixedRoute(HasChipLocation chip, AppID appID)
			throws IOException, ProcessException {
		return synchronousCall(new FixedRouteRead(chip, appID)).getRoute();
	}
}
