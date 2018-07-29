package uk.ac.manchester.spinnaker.connections.selectors;

import static java.util.Collections.unmodifiableList;

import java.util.List;

import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/**
 * A selector that spreads messages across all the connections it has. It uses a
 * simple round-robin algorithm for doing so; it does not take special care to
 * do so wisely.
 *
 * @param <T>
 *            The type of connection this selects.
 */
public final class RoundRobinConnectionSelector<T extends Connection>
		implements ConnectionSelector<T> {
	private final List<T> connections;
	private int next;

	/**
	 * @param connections
	 *            The list of connections that this selector iterates over.
	 */
	public RoundRobinConnectionSelector(List<T> connections) {
		if (connections.isEmpty()) {
			throw new IllegalArgumentException(
					"at least one connection must be provided");
		}
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
