package uk.ac.manchester.spinnaker.connections.selectors;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.spinnaker.connections.model.SCPSenderReceiver;
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
		for (C connection : connections) {
			if (connection.getChip().equals(ROOT)) {
				firstConnection = connection;
			}
			this.connections.put(connection.getChip(), connection);
		}
		if (firstConnection == null) {
			firstConnection = connections.iterator().next();
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
		C conn = connections.get(machine.getChipAt(destination).nearestEthernet
				.asChipLocation());
		return (conn == null) ? defaultConnection : conn;
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
