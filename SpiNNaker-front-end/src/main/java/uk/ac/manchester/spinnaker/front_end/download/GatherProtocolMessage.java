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
package uk.ac.manchester.spinnaker.front_end.download;

import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_NOT_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.messages.sdp.SDPPort;

/**
 * A message participating in the fast-data-download protocol. This protocol
 * looks like SDP in general.
 *
 * @author Donal Fellows
 */
public abstract class GatherProtocolMessage extends SDPMessage {
	/**
	 * Create a protocol message.
	 *
	 * @param destination
	 *            Where to send the message
	 * @param destPort
	 *            Which SDP port to send the message to
	 * @param payload
	 *            What the contents of the message should be.
	 */
	protected GatherProtocolMessage(HasCoreLocation destination,
			SDPPort destPort, ByteBuffer payload) {
		super(new SDPHeader(REPLY_NOT_EXPECTED, destination, destPort),
				payload);
	}

	/** The various IDs of messages used in the fast download protocol. */
	public enum ID {
		/** ID of message used to start sending data. */
		START_SENDING_DATA(100),
		/** ID of message used to start sending missing sequence numbers. */
		START_MISSING_SEQS(1000),
		/** ID of message used to send more missing sequence numbers. */
		NEXT_MISSING_SEQS(1001),
		/**
		 * ID of the clear message used to stop the extra monitor transmitting.
		 */
		CLEAR_TRANSMISSIONS(2000);

		/** The value of the ID. */
		public final int value;

		ID(int value) {
			this.value = value;
		}
	}
}
