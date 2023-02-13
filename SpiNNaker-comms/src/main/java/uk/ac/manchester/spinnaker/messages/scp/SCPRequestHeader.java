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
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SequenceNumberSource.getNextSequenceNumber;

import java.nio.ByteBuffer;
import java.util.Set;

import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/**
 * Represents the header of an SCP Request.
 * <p>
 * The sequence number, if zero, will be set by the message sending code to the
 * actual sequence number when the message is sent on a connection.
 */
public final class SCPRequestHeader implements SerializableMessage {
	/** The command of the SCP packet. */
	public final CommandCode command;

	/** The sequence number of the packet, between 0 and 65535. */
	private short sequence;

	private boolean sequenceSet;

	/**
	 * Create an instance that doesn't yet have a sequence number set.
	 *
	 * @param command
	 *            The command to perform.
	 * @see #issueSequenceNumber(Set)
	 */
	SCPRequestHeader(CommandCode command) {
		this.command = command;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This must not be called until after a
	 * {@linkplain #issueSequenceNumber(Set) sequence number is set}.
	 */
	@Override
	public void addToBuffer(ByteBuffer buffer) {
		buffer.putShort(command.getValue());
		if (!sequenceSet) {
			throw new IllegalStateException("sequence number not set");
		}
		buffer.putShort(sequence);
	}

	/**
	 * Set the sequence number of this request to the next available number.
	 * This can only ever be called once per request.
	 *
	 * @param inFlight
	 *            What sequence numbers are current in use and shouldn't be
	 *            used.
	 * @return The number that was issued.
	 * @throws IllegalStateException
	 *             If an attempt is made to set a sequence number a second time
	 */
	public short issueSequenceNumber(Set<Integer> inFlight) {
		if (sequenceSet) {
			throw new IllegalStateException(
					"a message can only have its sequence number set once");
		}
		do {
			sequence = getNextSequenceNumber();
		} while (inFlight.contains((int) sequence));
		sequenceSet = true;
		return sequence;
	}

	/** @return The message's sequence number. */
	public short getSequence() {
		return sequence;
	}
}
