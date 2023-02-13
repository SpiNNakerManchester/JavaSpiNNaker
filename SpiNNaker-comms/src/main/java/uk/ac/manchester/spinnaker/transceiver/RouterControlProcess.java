/*
 * Copyright (c) 2019-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.transceiver;

import static java.util.Collections.unmodifiableMap;
import static uk.ac.manchester.spinnaker.transceiver.CommonMemoryLocations.ROUTER_CONTROL;
import static uk.ac.manchester.spinnaker.transceiver.CommonMemoryLocations.ROUTER_DIAGNOSTICS;
import static uk.ac.manchester.spinnaker.transceiver.CommonMemoryLocations.ROUTER_ERROR;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus;
import uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics;
import uk.ac.manchester.spinnaker.messages.scp.ClearReinjectionQueue;
import uk.ac.manchester.spinnaker.messages.scp.GetReinjectionStatus;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.messages.scp.ResetReinjectionCounters;
import uk.ac.manchester.spinnaker.messages.scp.LoadApplicationRoutes;
import uk.ac.manchester.spinnaker.messages.scp.LoadSystemRoutes;
import uk.ac.manchester.spinnaker.messages.scp.SaveApplicationRoutes;
import uk.ac.manchester.spinnaker.messages.scp.SetReinjectionPacketTypes;
import uk.ac.manchester.spinnaker.messages.scp.SetRouterEmergencyTimeout;
import uk.ac.manchester.spinnaker.messages.scp.SetRouterTimeout;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * Access to the control facilities for a set of routers. Depends on access to
 * the extra monitor cores running on those chips.
 */
class RouterControlProcess extends TxrxProcess {
	private static final int REGISTER = 4;

	private static final int NUM_REGISTERS = 16;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	RouterControlProcess(
			ConnectionSelector<? extends SCPConnection> connectionSelector,
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void clearQueue(CoreLocation monitorCore)
			throws IOException, ProcessException, InterruptedException {
		call(new ClearReinjectionQueue(monitorCore));
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void clearQueue(CoreSubsets monitorCoreSubsets)
			throws IOException, ProcessException, InterruptedException {
		for (var core : monitorCoreSubsets) {
			sendRequest(new ClearReinjectionQueue(core));
		}
		finishBatch();
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void resetCounters(CoreLocation monitorCore)
			throws IOException, ProcessException, InterruptedException {
		call(new ResetReinjectionCounters(monitorCore));
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void resetCounters(CoreSubsets coreSubsets)
			throws IOException, ProcessException, InterruptedException {
		for (var core : coreSubsets) {
			sendRequest(new ResetReinjectionCounters(core));
		}
		finishBatch();
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void setPacketTypes(CoreLocation monitorCore, boolean multicast,
			boolean pointToPoint, boolean fixedRoute, boolean nearestNeighbour)
			throws IOException, ProcessException, InterruptedException {
		call(new SetReinjectionPacketTypes(monitorCore, multicast,
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void setPacketTypes(CoreSubsets monitorCoreSubsets, boolean multicast,
			boolean pointToPoint, boolean fixedRoute, boolean nearestNeighbour)
			throws IOException, ProcessException, InterruptedException {
		for (var core : monitorCoreSubsets) {
			sendRequest(new SetReinjectionPacketTypes(core, multicast,
					pointToPoint, fixedRoute, nearestNeighbour));
		}
		finishBatch();
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void setTimeout(CoreLocation monitorCore, int timeoutMantissa,
			int timeoutExponent)
			throws IOException, ProcessException, InterruptedException {
		call(new SetRouterTimeout(monitorCore, timeoutMantissa,
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void setTimeout(CoreSubsets monitorCoreSubsets, int timeoutMantissa,
			int timeoutExponent)
			throws IOException, ProcessException, InterruptedException {
		for (var core : monitorCoreSubsets) {
			sendRequest(new SetRouterTimeout(core, timeoutMantissa,
					timeoutExponent));
		}
		finishBatch();
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void setEmergencyTimeout(CoreLocation monitorCore, int timeoutMantissa,
			int timeoutExponent)
			throws IOException, ProcessException, InterruptedException {
		call(new SetRouterEmergencyTimeout(monitorCore,
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void setEmergencyTimeout(CoreSubsets monitorCoreSubsets,
			int timeoutMantissa, int timeoutExponent)
			throws IOException, ProcessException, InterruptedException {
		for (var core : monitorCoreSubsets) {
			sendRequest(new SetRouterEmergencyTimeout(core, timeoutMantissa,
					timeoutExponent));
		}
		finishBatch();
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void saveApplicationRouterTable(CoreLocation monitorCore)
			throws IOException, ProcessException, InterruptedException {
		call(new SaveApplicationRoutes(monitorCore));
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void saveApplicationRouterTable(CoreSubsets monitorCoreSubsets)
			throws IOException, ProcessException, InterruptedException {
		for (var core : monitorCoreSubsets) {
			sendRequest(new SaveApplicationRoutes(core));
		}
		finishBatch();
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void loadSystemRouterTable(CoreLocation monitorCore)
			throws IOException, ProcessException, InterruptedException {
		call(new LoadSystemRoutes(monitorCore));
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void loadSystemRouterTable(CoreSubsets monitorCoreSubsets)
			throws IOException, ProcessException, InterruptedException {
		for (var core : monitorCoreSubsets) {
			sendRequest(new LoadSystemRoutes(core));
		}
		finishBatch();
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void loadApplicationRouterTable(CoreLocation monitorCore)
			throws IOException, ProcessException, InterruptedException {
		call(new LoadApplicationRoutes(monitorCore));
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void loadApplicationRouterTable(CoreSubsets monitorCoreSubsets)
			throws IOException, ProcessException, InterruptedException {
		for (var core : monitorCoreSubsets) {
			sendRequest(new LoadApplicationRoutes(core));
		}
		finishBatch();
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	ReinjectionStatus getReinjectionStatus(CoreLocation monitorCore)
			throws IOException, ProcessException, InterruptedException {
		return retrieve(new GetReinjectionStatus(monitorCore));
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	Map<CoreLocation, ReinjectionStatus> getReinjectionStatus(
			CoreSubsets monitorCoreSubsets)
			throws IOException, ProcessException, InterruptedException {
		var status = new HashMap<CoreLocation, ReinjectionStatus>();
		for (var core : monitorCoreSubsets) {
			sendGet(new GetReinjectionStatus(core), s -> status.put(core, s));
		}
		finishBatch();
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	RouterDiagnostics getRouterDiagnostics(HasChipLocation chip)
			throws IOException, ProcessException, InterruptedException {
		var cr = new ValueHolder<Integer>();
		var es = new ValueHolder<Integer>();
		var reg = new int[NUM_REGISTERS];

		sendGet(new ReadMemory(chip, ROUTER_CONTROL, REGISTER),
				bytes -> cr.setValue(bytes.getInt()));
		sendGet(new ReadMemory(chip, ROUTER_ERROR, REGISTER),
				bytes -> es.setValue(bytes.getInt()));
		sendGet(new ReadMemory(chip, ROUTER_DIAGNOSTICS,
				NUM_REGISTERS * REGISTER),
				bytes -> bytes.asIntBuffer().get(reg));

		finishBatch();
		return new RouterDiagnostics(cr.getValue(), es.getValue(), reg);
	}
}
