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

import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_NOT_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

/**
 * A message participating in the fast-data-download protocol. This protocol
 * looks like SDP in general.
 *
 * @author Donal Fellows
 */
public abstract class ProtocolMessage extends SDPMessage {
	/**
	 * Create a protocol message.
	 *
	 * @param destination
	 *            Where to send the message
	 * @param destPort
	 *            Which port to send the message to
	 * @param payload
	 *            What the contents of the message should be.
	 */
	protected ProtocolMessage(HasCoreLocation destination, int destPort,
			ByteBuffer payload) {
		super(new SDPHeader(REPLY_NOT_EXPECTED, destination, destPort),
            payload);
	}
}
