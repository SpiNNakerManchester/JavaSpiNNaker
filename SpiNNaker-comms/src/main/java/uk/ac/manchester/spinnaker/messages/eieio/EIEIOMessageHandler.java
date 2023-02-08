/*
 * Copyright (c) 2020 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.eieio;

import uk.ac.manchester.spinnaker.connections.model.MessageHandler;

/**
 * Handles a message received from an EIEIO connection.
 *
 * @author Donal Fellows
 */
public interface EIEIOMessageHandler
		extends MessageHandler<EIEIOMessage<? extends EIEIOHeader>> {
	@Override
	default void handle(EIEIOMessage<? extends EIEIOHeader> message) {
		if (message instanceof EIEIOCommandMessage cmd) {
			handleCommand(cmd);
		} else if (message instanceof EIEIODataMessage data) {
			handleData(data);
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
