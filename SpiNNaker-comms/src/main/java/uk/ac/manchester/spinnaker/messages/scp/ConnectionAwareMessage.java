/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.scp;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/**
 * An interface that is applied to a message when it wants to be told what
 * connection it is being sent down prior to it being sent. This is intended to
 * allow information about the connection to be used in producing the parsed
 * response object.
 *
 * @author Donal Fellows
 */
public interface ConnectionAwareMessage extends SerializableMessage {
	/**
	 * Tell the object what connection it is being sent on. This will usually
	 * (except during testing) be called prior to issuing the sequence number,
	 * but the information provided should not be used until the response is
	 * being parsed.
	 *
	 * @param connection
	 *            The connection. Should <em>not</em> be modified by this
	 *            method.
	 */
	void setConnection(SCPConnection connection);
}
