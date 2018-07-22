package uk.ac.manchester.spinnaker.selectors;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/** A selector that goes for the most direct connection for the message. */
public final class MostDirectConnectionSelector implements ConnectionSelector {
	private static final ChipLocation ROOT = new ChipLocation(0, 0);
	private final Map<ChipLocation, SCPConnection> connections;
	private final SCPConnection defaultConnection;
	private Machine machine;

	public MostDirectConnectionSelector(Machine machine,
			Collection<SCPConnection> connections) {
		this.machine = machine;
		this.connections = new HashMap<>();
		SCPConnection first_connection = null;
		for (SCPConnection connection : connections) {
			if (connection.getChip().equals(ROOT)) {
				first_connection = connection;
			}
			this.connections.put(connection.getChip(), connection);
		}
		if (first_connection == null) {
			first_connection = connections.iterator().next();
		}
		this.defaultConnection = first_connection;
	}

	@Override
	public SCPConnection getNextConnection(SCPRequest<?> request) {
		if (machine == null || connections.size() == 1) {
			return defaultConnection;
		}
		SCPConnection conn = connections
				.get(machine.getChipAt(request.sdpHeader.getDestination()
						.asChipLocation()).nearestEthernet.asChipLocation());
		return (conn == null) ? defaultConnection : conn;
	}

	public Machine getMachine() {
		return machine;
	}

	public void setMachine(Machine machine) {
		this.machine = machine;
	}
}
