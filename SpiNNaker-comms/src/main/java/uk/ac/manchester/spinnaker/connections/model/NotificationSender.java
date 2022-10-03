/*
 * Copyright (c) 2022 The University of Manchester
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

import uk.ac.manchester.spinnaker.messages.notification.NotificationMessage;

/** A sender of notification protocol messages. */
public interface NotificationSender extends Connection {
	/**
	 * Sends a notification message down this connection.
	 *
	 * @param notificationMessage
	 *            The notification message to be sent
	 * @throws IOException
	 *             If there is an error sending the message
	 */
	void sendNotification(NotificationMessage notificationMessage)
			throws IOException;
}