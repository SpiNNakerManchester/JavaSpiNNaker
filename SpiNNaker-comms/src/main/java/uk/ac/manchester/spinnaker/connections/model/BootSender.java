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

import uk.ac.manchester.spinnaker.messages.boot.BootMessage;

/** A sender of SpiNNaker Boot messages. */
public interface BootSender extends SocketHolder {
	/**
	 * Sends a SpiNNaker boot message using this connection.
	 *
	 * @param bootMessage
	 *            The message to be sent
	 * @throws IOException
	 *             If there is an error sending the message
	 */
	void sendBootMessage(BootMessage bootMessage) throws IOException;
}
