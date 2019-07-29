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
package uk.ac.manchester.spinnaker.transceiver;

import static java.util.Collections.unmodifiableMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus;
import uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics;
import uk.ac.manchester.spinnaker.messages.scp.ClearReinjectionQueue;
import uk.ac.manchester.spinnaker.messages.scp.GetReinjectionStatus;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.messages.scp.ResetReinjectionCounters;
import uk.ac.manchester.spinnaker.messages.scp.RouterTableLoadApplicationRoutes;
import uk.ac.manchester.spinnaker.messages.scp.RouterTableLoadSystemRoutes;
import uk.ac.manchester.spinnaker.messages.scp.RouterTableSaveApplicationRoutes;
import uk.ac.manchester.spinnaker.messages.scp.SetReinjectionPacketTypes;
import uk.ac.manchester.spinnaker.messages.scp.SetRouterEmergencyTimeout;
import uk.ac.manchester.spinnaker.messages.scp.SetRouterTimeout;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * Access to the control facilities for a set of routers. Depends on access to
 * the extra monitor cores running on those chips.
 */
class RouterControlProcess extends MultiConnectionProcess<SCPConnection> {
	private static final int REGISTER = 4;
	private static final int NUM_REGISTERS = 16;
	private static final int ROUTER_CONTROL_REGISTER = 0xe1000000;
	private static final int ROUTER_ERROR_STATUS = 0xe1000014;
	private static final int ROUTER_REGISTERS = 0xe1000300;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	RouterControlProcess(ConnectionSelector<SCPConnection> connectionSelector,
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
	void clearQueue(CoreLocation monitorCore)
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
	void clearQueue(CoreSubsets monitorCoreSubsets)
			throws IOException, ProcessException {
		for (CoreLocation core : monitorCoreSubsets) {
			sendRequest(new ClearReinjectionQueue(core));
		}
		finish();
		checkForError();
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
	void resetCounters(CoreLocation monitorCore)
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
	void resetCounters(CoreSubsets coreSubsets)
			throws IOException, ProcessException {
		for (CoreLocation core : coreSubsets) {
			sendRequest(new ResetReinjectionCounters(core));
		}
		finish();
		checkForError();
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
	void setPacketTypes(CoreLocation monitorCore, boolean multicast,
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
	void setPacketTypes(CoreSubsets monitorCoreSubsets, boolean multicast,
			boolean pointToPoint, boolean fixedRoute, boolean nearestNeighbour)
			throws IOException, ProcessException {
		for (CoreLocation core : monitorCoreSubsets) {
			sendRequest(new SetReinjectionPacketTypes(core, multicast,
					pointToPoint, fixedRoute, nearestNeighbour));
		}
		finish();
		checkForError();
	}

	/**
	 * Set the timeout in an (extra) monitor's router.
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
	void setTimeout(CoreLocation monitorCore, int timeoutMantissa,
			int timeoutExponent) throws IOException, ProcessException {
		synchronousCall(new SetRouterTimeout(monitorCore, timeoutMantissa,
				timeoutExponent));
	}

	/**
	 * Set the timeout in a set of (extra) monitors' routers.
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
	void setTimeout(CoreSubsets monitorCoreSubsets, int timeoutMantissa,
			int timeoutExponent) throws IOException, ProcessException {
		for (CoreLocation core : monitorCoreSubsets) {
			sendRequest(new SetRouterTimeout(core, timeoutMantissa,
					timeoutExponent));
		}
		finish();
		checkForError();
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
	void setEmergencyTimeout(CoreLocation monitorCore, int timeoutMantissa,
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
	void setEmergencyTimeout(CoreSubsets monitorCoreSubsets,
			int timeoutMantissa, int timeoutExponent)
			throws IOException, ProcessException {
		for (CoreLocation core : monitorCoreSubsets) {
			sendRequest(new SetRouterEmergencyTimeout(core, timeoutMantissa,
					timeoutExponent));
		}
		finish();
		checkForError();
	}

	/**
	 * Save the application's multicast router table for a particular router.
	 *
	 * @param monitorCore
	 *            the extra monitor core on the chip with the router.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	void saveApplicationRouterTable(CoreLocation monitorCore)
			throws IOException, ProcessException {
		synchronousCall(new RouterTableSaveApplicationRoutes(monitorCore));
	}

	/**
	 * Save the application's multicast router tables for a collection of
	 * routers.
	 *
	 * @param monitorCoreSubsets
	 *            the cores where the monitors are running.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	void saveApplicationRouterTable(CoreSubsets monitorCoreSubsets)
			throws IOException, ProcessException {
		for (CoreLocation core : monitorCoreSubsets) {
			sendRequest(new RouterTableSaveApplicationRoutes(core));
		}
		finish();
		checkForError();
	}

	/**
	 * Load the (previously configured into the extra monitor core) system
	 * multicast router table for a particular router.
	 *
	 * @param monitorCore
	 *            the extra monitor core on the chip with the router.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	void loadSystemRouterTable(CoreLocation monitorCore)
			throws IOException, ProcessException {
		synchronousCall(new RouterTableLoadSystemRoutes(monitorCore));
	}

	/**
	 * Load the (previously configured into the extra monitor cores) system
	 * multicast router tables for a collection of routers.
	 *
	 * @param monitorCoreSubsets
	 *            the extra monitor cores on the chips with the routers.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	void loadSystemRouterTable(CoreSubsets monitorCoreSubsets)
			throws IOException, ProcessException {
		for (CoreLocation core : monitorCoreSubsets) {
			sendRequest(new RouterTableLoadSystemRoutes(core));
		}
		finish();
		checkForError();
	}

	/**
	 * Load the (previously saved) application's multicast router table for a
	 * particular router.
	 *
	 * @param monitorCore
	 *            the extra monitor core on the chip with the router.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	void loadApplicationRouterTable(CoreLocation monitorCore)
			throws IOException, ProcessException {
		synchronousCall(new RouterTableLoadApplicationRoutes(monitorCore));
	}

	/**
	 * Load the (previously saved) application's multicast router tables for a
	 * collection of routers.
	 *
	 * @param monitorCoreSubsets
	 *            the extra monitor cores on the chips with the routers.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	void loadApplicationRouterTable(CoreSubsets monitorCoreSubsets)
			throws IOException, ProcessException {
		for (CoreLocation core : monitorCoreSubsets) {
			sendRequest(new RouterTableLoadApplicationRoutes(core));
		}
		finish();
		checkForError();
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
	ReinjectionStatus getReinjectionStatus(CoreLocation monitorCore)
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
	Map<CoreLocation, ReinjectionStatus> getReinjectionStatus(
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

	/**
	 * Get a chip's router's diagnostics.
	 *
	 * @param chip
	 *            The chip.
	 * @return The diagnostics from the chip's router.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	RouterDiagnostics getRouterDiagnostics(HasChipLocation chip)
			throws IOException, ProcessException {
		ValueHolder<Integer> cr = new ValueHolder<>();
		ValueHolder<Integer> es = new ValueHolder<>();
		int[] reg = new int[NUM_REGISTERS];

		sendRequest(new ReadMemory(chip, ROUTER_CONTROL_REGISTER, REGISTER),
				response -> cr.setValue(response.data.getInt()));
		sendRequest(new ReadMemory(chip, ROUTER_ERROR_STATUS, REGISTER),
				response -> es.setValue(response.data.getInt()));
		sendRequest(
				new ReadMemory(chip, ROUTER_REGISTERS,
						NUM_REGISTERS * REGISTER),
				response -> response.data.asIntBuffer().get(reg));

		finish();
		checkForError();
		return new RouterDiagnostics(cr.getValue(), es.getValue(), reg);
	}
}
