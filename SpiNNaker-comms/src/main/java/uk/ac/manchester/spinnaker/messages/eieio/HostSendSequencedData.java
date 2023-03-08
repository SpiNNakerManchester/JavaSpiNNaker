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

import static java.lang.Byte.toUnsignedInt;
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.HOST_SEND_SEQUENCED_DATA;
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessageFactory.readDataMessage;

import java.nio.ByteBuffer;

/**
 * Packet sent from the host to the SpiNNaker system in the context of buffering
 * input mechanism to identify packet which needs to be stored in memory for
 * future use.
 */
public class HostSendSequencedData extends EIEIOCommandMessage {
	/** What region will be moved. */
	public final int regionID;

	/** The message sequence number. */
	public final int sequenceNum;

	/** The data. */
	public final EIEIODataMessage eieioDataMessage;

	/** The length of the payload of the message. */
	private static final int PAYLOAD_LENGTH = 2;

	/**
	 * Create a message.
	 *
	 * @param regionID
	 *            The region ID
	 * @param sequenceNum
	 *            The sequence number
	 * @param eieioDataMessage
	 *            The payload data.
	 */
	public HostSendSequencedData(byte regionID, byte sequenceNum,
			EIEIODataMessage eieioDataMessage) {
		super(HOST_SEND_SEQUENCED_DATA);
		this.regionID = regionID;
		this.sequenceNum = sequenceNum;
		this.eieioDataMessage = eieioDataMessage;
	}

	HostSendSequencedData(ByteBuffer data) {
		super(data);

		regionID = toUnsignedInt(data.get());
		sequenceNum = toUnsignedInt(data.get());
		eieioDataMessage = readDataMessage(data);
	}

	@Override
	public int minPacketLength() {
		return super.minPacketLength() + PAYLOAD_LENGTH;
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put((byte) regionID);
		buffer.put((byte) sequenceNum);
		eieioDataMessage.addToBuffer(buffer);
	}
}
