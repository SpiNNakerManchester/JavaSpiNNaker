package uk.ac.manchester.spinnaker.selectors;

import static java.util.Collections.unmodifiableList;

import java.util.List;

import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/** A selector that spreads messages across all the connections it has. */
public final class RoundRobinConnectionSelector<T extends Connection>
		implements ConnectionSelector<T> {
	private final List<T> connections;
	private int next;

	public RoundRobinConnectionSelector(List<T> connections) {
		this.connections = unmodifiableList(connections);
		next = 0;
	}

	@Override
	public T getNextConnection(SCPRequest<?> request) {
		int idx = next;
		next = (idx + 1) % connections.size();
		return connections.get(idx);
	}
}
