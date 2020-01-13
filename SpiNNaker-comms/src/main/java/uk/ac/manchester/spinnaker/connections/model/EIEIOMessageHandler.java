/*
 * Copyright (c) 2020 The University of Manchester
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

import uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandMessage;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIODataMessage;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOHeader;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessage;

/**
 * Handles a message received from an EIEIO connection.
 *
 * @author Donal Fellows
 */
public interface EIEIOMessageHandler
		extends MessageHandler<EIEIOMessage<? extends EIEIOHeader>> {
	@Override
	default void handle(EIEIOMessage<? extends EIEIOHeader> message) {
		if (message instanceof EIEIOCommandMessage) {
			handleCommand((EIEIOCommandMessage) message);
		} else if (message instanceof EIEIODataMessage) {
			handleData((EIEIODataMessage) message);
		} else {
			throw new IllegalArgumentException(
					"unsupported message type: " + message.getClass());
		}
	}

	/**
	 * Handle an EIEIO command message.
	 *
	 * @param message
	 *            The message that was received.
	 */
	void handleCommand(EIEIOCommandMessage message);


	/**
	 * Handle an EIEIO data message.
	 *
	 * @param message
	 *            The message that was received.
	 */
	void handleData(EIEIODataMessage message);
}
