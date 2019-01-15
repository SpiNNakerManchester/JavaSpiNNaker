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
package uk.ac.manchester.spinnaker.front_end.download;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.front_end.download.ProtocolID.START_SENDING_DATA;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * A message used to request fast data transfer from SpiNNaker to Host.
 */
@Deprecated
final class StartSendingMessage extends ProtocolMessage {
	private static final int NUM_WORDS = 3;

	/**
	 * Create a message used to request fast data transfer from SpiNNaker to
	 * Host.
	 *
	 * @param destination
	 *            Where to send the message
	 * @param destPort
	 *            Which port to send the message to
	 * @param address
	 *            Where to start reading from
	 * @param length
	 *            How many bytes to read
	 * @return The created message.
	 */
	static StartSendingMessage create(HasCoreLocation destination, int destPort,
			int address, int length) {
		ByteBuffer payload =
				allocate(NUM_WORDS * WORD_SIZE).order(LITTLE_ENDIAN);
		IntBuffer msgPayload = payload.asIntBuffer();
		msgPayload.put(START_SENDING_DATA.value);
		msgPayload.put(address);
		msgPayload.put(length);
		return new StartSendingMessage(destination, destPort, payload);
	}

	private StartSendingMessage(HasCoreLocation destination, int destPort,
			ByteBuffer payload) {
		super(destination, destPort, payload);
	}
}
