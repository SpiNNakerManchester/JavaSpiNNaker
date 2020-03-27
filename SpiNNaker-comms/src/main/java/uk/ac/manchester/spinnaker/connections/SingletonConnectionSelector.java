/*
 * Copyright (c) 2018 The University of Manchester
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

import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/**
 * A selector that only ever handles a single connection.
 *
 * @param <T>
 *            The type of the connection.
 */
public class SingletonConnectionSelector<T extends Connection>
		implements ConnectionSelector<T> {
	private final T connection;

	/**
	 * Create a selector.
	 *
	 * @param connection
	 *            The connection in the selector.
	 */
	public SingletonConnectionSelector(T connection) {
		this.connection = connection;
	}

	@Override
	public T getNextConnection(SCPRequest<?> request) {
		// Ignores the request, always
		return connection;
	}
}
