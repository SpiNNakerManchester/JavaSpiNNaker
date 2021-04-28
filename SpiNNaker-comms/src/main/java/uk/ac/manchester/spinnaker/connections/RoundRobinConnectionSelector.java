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
package uk.ac.manchester.spinnaker.connections;

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
	 * @throws IllegalArgumentException
	 *             If the list of connections is empty.
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
		try {
			return connections.get(next);
		} finally {
			next = (next + 1) % connections.size();
		}
	}
}
