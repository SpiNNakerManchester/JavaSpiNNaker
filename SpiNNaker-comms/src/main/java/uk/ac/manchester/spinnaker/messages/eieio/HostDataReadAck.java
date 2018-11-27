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
