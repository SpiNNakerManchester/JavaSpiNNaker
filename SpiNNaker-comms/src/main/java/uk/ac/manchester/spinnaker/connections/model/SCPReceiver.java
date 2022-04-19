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
package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;
import java.net.SocketTimeoutException;

import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;

/**
 * Receives an SCP response from a socket.
 *
 * @author Donal Fellows
 */
public interface SCPReceiver extends SocketHolder {
	/**
	 * Receives an SCP response from this connection. Blocks until a message has
	 * been received, or a timeout occurs.
	 *
	 * @param timeout
	 *            The time in milliseconds to wait for the message to arrive; if
	 *            {@code null}, will wait forever, or until the connection is
	 *            closed.
	 * @return The SCP result, the sequence number, and the data of the
	 *         response. The buffer pointer will be positioned at the point
	 *         where the payload starts.
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws SocketTimeoutException
	 *             If there is a timeout before a message is received
	 */
	default SCPResultMessage receiveSCPResponse(Integer timeout)
			throws SocketTimeoutException, IOException {
		return receiveSCPResponse(convertTimeout(timeout));
	}

	/**
	 * Receives an SCP response from this connection. Blocks until a message has
	 * been received, or a timeout occurs.
	 *
	 * @param timeout
	 *            The time in milliseconds to wait for the message to arrive, or
	 *            until the connection is closed.
	 * @return The SCP result, the sequence number, and the data of the
	 *         response. The buffer pointer will be positioned at the point
	 *         where the payload starts.
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws SocketTimeoutException
	 *             If there is a timeout before a message is received
	 */
	SCPResultMessage receiveSCPResponse(int timeout)
			throws SocketTimeoutException, IOException;
}
