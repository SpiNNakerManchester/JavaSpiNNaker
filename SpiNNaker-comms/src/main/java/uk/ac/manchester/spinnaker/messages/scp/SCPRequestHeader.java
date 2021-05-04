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
public class SCPRequestHeader implements SerializableMessage {
	/** The command of the SCP packet. */
	public final CommandCode command;

	/** The sequence number of the packet, between 0 and 65535. */
	private short sequence;

	private boolean sequenceSet;

	public SCPRequestHeader(CommandCode command) {
		this.command = command;
	}

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

	public short getSequence() {
		return sequence;
	}
}
