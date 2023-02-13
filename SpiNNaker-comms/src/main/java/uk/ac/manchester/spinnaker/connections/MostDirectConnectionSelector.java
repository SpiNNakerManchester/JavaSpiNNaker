/*
 * Copyright (c) 2018-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.connections;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.model.SCPSenderReceiver;
import uk.ac.manchester.spinnaker.machine.Chip;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/**
 * A selector that goes for the most direct connection for the message.
 *
 * @param <C>
 *            The type of connections selected between.
 */
public final class MostDirectConnectionSelector<C extends SCPSenderReceiver>
		implements ConnectionSelector<C>, MachineAware {
	private static final Logger log =
			getLogger(MostDirectConnectionSelector.class);

	private static final ChipLocation ROOT = new ChipLocation(0, 0);

	private final Map<ChipLocation, C> connections;

	private final C defaultConnection;

	private Machine machine;

	/**
	 * Create a selector.
	 *
	 * @param machine
	 *            The machine, used to work out efficient routing strategies.
	 * @param connections
	 *            The connections that can be chosen between.
	 */
	public MostDirectConnectionSelector(Machine machine,
			Collection<C> connections) {
		this.machine = machine;
		this.connections = new HashMap<>();
		C firstConnection = null;
		for (var conn : connections) {
			if (firstConnection == null || conn.getChip().equals(ROOT)) {
				firstConnection = conn;
			}
			this.connections.put(conn.getChip(), conn);
		}
		this.defaultConnection = firstConnection;
	}

	@Override
	public C getNextConnection(SCPRequest<?> request) {
		if (machine == null || connections.size() == 1) {
			return defaultConnection;
		}
		var destination = request.sdpHeader.getDestination().asChipLocation();
		var routeVia = machine.getChipAt(destination).nearestEthernet;
		var conn = connections.get(routeVia);
		if (log.isDebugEnabled()) {
			if (conn != null) {
				log.debug("will route packets for {} via {}", destination,
						routeVia);
			} else {
				log.debug("will route packets for {} via the "
						+ "default connecttion", destination);
			}
		}
		return (conn == null) ? defaultConnection : conn;
	}

	/**
	 * Tests if this connection selector will be able to make a direct
	 * connection to the given ethernet chip.
	 *
	 * @param ethernetChip
	 *            The ethernet chip that we are testing for direct routing to.
	 * @return True iff we can talk directly to it using a connection that this
	 *         selector knows about.
	 */
	public boolean hasDirectConnectionFor(Chip ethernetChip) {
		return connections.containsKey(ethernetChip.asChipLocation());
	}

	@Override
	public Machine getMachine() {
		return machine;
	}

	@Override
	public void setMachine(Machine machine) {
		this.machine = machine;
	}
}
