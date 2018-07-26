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
 * @param <Connection>
 *            The type of connections selected between.
 */
public final class MostDirectConnectionSelector<Connection extends SCPSenderReceiver>
		implements ConnectionSelector<Connection>, MachineAware {
	private static final ChipLocation ROOT = new ChipLocation(0, 0);
	private final Map<ChipLocation, Connection> connections;
	private final Connection defaultConnection;
	private Machine machine;

	public MostDirectConnectionSelector(Machine machine,
			Collection<Connection> connections) {
		this.machine = machine;
		this.connections = new HashMap<>();
		Connection firstConnection = null;
		for (Connection connection : connections) {
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
	public Connection getNextConnection(SCPRequest<?> request) {
		if (machine == null || connections.size() == 1) {
			return defaultConnection;
		}
		Connection conn = connections
				.get(machine.getChipAt(request.sdpHeader.getDestination()
						.asChipLocation()).nearestEthernet.asChipLocation());
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
