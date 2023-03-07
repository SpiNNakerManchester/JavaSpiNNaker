/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
