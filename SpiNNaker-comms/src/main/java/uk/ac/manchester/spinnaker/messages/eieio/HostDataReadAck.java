/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.HOST_DATA_READ_ACK;

import java.nio.ByteBuffer;

/**
 * Packet sent by the host computer to the SpiNNaker system in the context of
 * the buffering output technique to signal that the host has received a request
 * to read data.
 *
 * @see HostDataRead
 */
public class HostDataReadAck extends EIEIOCommandMessage {
	/** The message sequence number that is being acknowledged. */
	public final byte sequenceNumber;

	/**
	 * Create.
	 *
	 * @param sequenceNumber
	 *            The message sequence number that is being acknowledged.
	 */
	public HostDataReadAck(byte sequenceNumber) {
		super(HOST_DATA_READ_ACK);
		this.sequenceNumber = sequenceNumber;
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put(sequenceNumber);
	}
}
