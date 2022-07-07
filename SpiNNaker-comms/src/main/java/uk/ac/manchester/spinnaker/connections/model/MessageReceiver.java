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

/**
 * A receiver of SpiNNaker messages.
 *
 * @param <MessageType>
 *            The type of message to be received. It's possible for the received
 *            information to even be metadata about the message, and not the
 *            content of the message.
 * @author Donal Fellows
 */
public interface MessageReceiver<MessageType> extends SocketHolder {
	/**
	 * Receives a SpiNNaker message from this connection. Blocks until a message
	 * has been received.
	 *
	 * @return the received message
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws IllegalArgumentException
	 *             If one of the fields of the SpiNNaker message is invalid
	 */
	default MessageType receiveMessage() throws IOException {
		return receiveMessage(0);
	}

	/**
	 * Receives a SpiNNaker message from this connection. Blocks until a message
	 * has been received, or a timeout occurs.
	 *
	 * @param timeout
	 *            The time in seconds to wait for the message to arrive, or
	 *            until the connection is closed.
	 * @return the received message
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws SocketTimeoutException
	 *             If there is a timeout during receiving
	 * @throws IllegalArgumentException
	 *             If one of the fields of the SpiNNaker message is invalid
	 */
	MessageType receiveMessage(int timeout) throws IOException;
}
