/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
class FixedRouteControlProcess extends TxrxProcess {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	FixedRouteControlProcess(
			ConnectionSelector<? extends SCPConnection> connectionSelector,
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void loadFixedRoute(HasChipLocation chip, RoutingEntry fixedRoute)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void loadFixedRoute(HasChipLocation chip, RoutingEntry fixedRoute,
			AppID appID)
			throws IOException, ProcessException, InterruptedException {
		int entry = fixedRoute.encode();
		call(new FixedRouteInitialise(chip, entry, appID));
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	RoutingEntry readFixedRoute(HasChipLocation chip)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	RoutingEntry readFixedRoute(HasChipLocation chip, AppID appID)
			throws IOException, ProcessException, InterruptedException {
		return retrieve(new FixedRouteRead(chip, appID));
	}
}
