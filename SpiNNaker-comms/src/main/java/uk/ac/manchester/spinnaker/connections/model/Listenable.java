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
 * How to listen for a message on a connection.
 *
 * @param <MessageType>
 *            The type of message being listened for (typically fixed for a
 *            particular connection).
 */
public interface Listenable<MessageType> extends MessageReceiver<MessageType> {
	/**
	 * Do a non-blocking poll of whether there is a message ready to be received
	 * without blocking.
	 *
	 * @return true when there is a packet waiting to be received
	 * @throws IOException
	 *             If anything goes wrong, e.g., if the socket is closed under
	 *             our feet.
	 * @see #isReadyToReceive(Integer)
	 */
	default boolean isReadyToReceive() throws IOException {
		return isReadyToReceive(null);
	}

	/**
	 * Determines if there is a message available to be received without
	 * blocking. <i>This method</i> may block until the timeout given.
	 *
	 * @param timeout
	 *            How long to wait, in <em>milliseconds</em>; if zero or
	 *            {@code null}, a non-blocking poll is performed.
	 * @return true when there is a message waiting to be received
	 * @throws IOException
	 *             If anything goes wrong, e.g., if the socket is closed under
	 *             our feet.
	 */
	boolean isReadyToReceive(Integer timeout) throws IOException;
}
