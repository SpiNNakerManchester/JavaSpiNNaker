package uk.ac.manchester.spinnaker.selectors;

import static java.util.Collections.unmodifiableList;

import java.util.List;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/** A selector that spreads messages across all the connections it has. */
public final class RoundRobinConnectionSelector implements ConnectionSelector {
	private final List<SCPConnection> connections;
	private int next_connection_index;

	public RoundRobinConnectionSelector(List<SCPConnection> connections) {
		this.connections = unmodifiableList(connections);
		next_connection_index = 0;
	}

	@Override
	public SCPConnection getNextConnection(SCPRequest<?> request) {
		int idx = next_connection_index;
		next_connection_index = (idx + 1) % connections.size();
		return connections.get(idx);
	}
}
