/*
 * Copyright (c) 2023 The University of Manchester
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
