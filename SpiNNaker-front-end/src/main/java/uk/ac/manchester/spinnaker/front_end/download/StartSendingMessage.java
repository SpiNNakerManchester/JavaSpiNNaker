/*
 * Copyright (c) 2018-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.download;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.front_end.download.GatherProtocolMessage.ID.START_SENDING_DATA;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.EXTRA_MONITOR_CORE_DATA_SPEED_UP;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;

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
	static StartSendingMessage create(HasCoreLocation destination,
			MemoryLocation address, int length, int transactionId) {
		var payload = allocate(NUM_WORDS * WORD_SIZE).order(LITTLE_ENDIAN);
		payload.putInt(START_SENDING_DATA.value);
		payload.putInt(transactionId);
		payload.putInt(address.address);
		payload.putInt(length);
		payload.flip();
		return new StartSendingMessage(destination, payload);
	}

	private StartSendingMessage(HasCoreLocation destination,
			ByteBuffer payload) {
		super(destination, EXTRA_MONITOR_CORE_DATA_SPEED_UP, payload);
	}
}
