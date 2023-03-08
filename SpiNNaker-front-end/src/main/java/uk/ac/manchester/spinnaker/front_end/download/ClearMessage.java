/*
 * Copyright (c) 2020 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import static uk.ac.manchester.spinnaker.front_end.download.GatherProtocolMessage.ID.CLEAR_TRANSMISSIONS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.EXTRA_MONITOR_CORE_DATA_SPEED_UP;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * A message used to request fast data transfer from SpiNNaker to Host.
 *
 * @author Alan Stokes
 */

public final class ClearMessage extends GatherProtocolMessage {
	private static final int NUM_WORDS = 2;

	/**
	 * Create a message used to request fast data transfer from SpiNNaker to
	 * Host.
	 *
	 * @param destination
	 *            Where to send the message
	 * @param transactionId
	 *            the transaction id needed
	 * @return The created message.
	 */
	static ClearMessage create(HasCoreLocation destination, int transactionId) {
		var payload = allocate(NUM_WORDS * WORD_SIZE).order(LITTLE_ENDIAN);
		payload.putInt(CLEAR_TRANSMISSIONS.value);
		payload.putInt(transactionId);
		payload.flip();
		return new ClearMessage(destination, payload);
	}

	private ClearMessage(HasCoreLocation destination, ByteBuffer payload) {
		super(destination, EXTRA_MONITOR_CORE_DATA_SPEED_UP, payload);
	}
}
