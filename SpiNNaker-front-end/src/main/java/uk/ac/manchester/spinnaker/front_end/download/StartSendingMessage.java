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
import static uk.ac.manchester.spinnaker.front_end.download.GatherProtocolMessage.ID.START_SENDING_DATA;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.EXTRA_MONITOR_CORE_DATA_SPEED_UP;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * A message used to request fast data transfer from SpiNNaker to Host.
 */
public final class StartSendingMessage extends GatherProtocolMessage {
	private static final int NUM_WORDS = 4;

	/**
	 * Create a message used to request fast data transfer from SpiNNaker to
	 * Host.
	 *
	 * @param destination
	 *            Where to send the message
	 * @param address
	 *            Where to start reading from
	 * @param length
	 *            How many bytes to read
	 * @param transactionId
	 *            the transaction ID needed
	 * @return The created message.
	 */
	static StartSendingMessage create(HasCoreLocation destination, int address,
			int length, int transactionId) {
		ByteBuffer payload =
				allocate(NUM_WORDS * WORD_SIZE).order(LITTLE_ENDIAN);
		payload.putInt(START_SENDING_DATA.value);
		payload.putInt(transactionId);
		payload.putInt(address);
		payload.putInt(length);
		payload.flip();
		return new StartSendingMessage(destination, payload);
	}

	private StartSendingMessage(HasCoreLocation destination,
			ByteBuffer payload) {
		super(destination, EXTRA_MONITOR_CORE_DATA_SPEED_UP, payload);
	}
}
