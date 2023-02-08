/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 * A connection selector for (especially multi-connection) processes.
 *
 * @param <T>
 *            The type of connections handled by this selector.
 */
public interface ConnectionSelector<T extends Connection> {
	/**
	 * Get the next connection for the process from a list of connections that
	 * might satisfy the request.
	 *
	 * @param request
	 *            The SCP message to be sent
	 * @return The connection on which the message should be sent.
	 */
	T getNextConnection(SCPRequest<?> request);
}
