/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.connections.selectors;

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
		for (C conn : connections) {
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
		ChipLocation destination = request.sdpHeader.getDestination()
				.asChipLocation();
		ChipLocation routeVia = machine.getChipAt(destination).nearestEthernet;
		C conn = connections.get(routeVia);
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

	/**
	 * Tests if this connection selector will be able to make a direct
	 * connection to the board containing a given chip.
	 *
	 * @param chip
	 *            A chip on the board that we are testing for direct routing to.
	 * @return True iff we can talk directly to the board using a connection
	 *         that this selector knows about.
	 */
	public boolean hasConnectionToBoardOf(Chip chip) {
		return hasDirectConnectionFor(machine.getChipAt(chip.nearestEthernet));
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
