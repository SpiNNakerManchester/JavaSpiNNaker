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

import static uk.ac.manchester.spinnaker.messages.scp.RunningCommand.CLEAR_IOBUF;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP Request to clear the IOBUF on a chip.
 */
public class ClearIOBUF extends SCPRequest<CheckOKResponse> {
	/**
	 * @param chip
	 *            The chip to set the tag on.
	 * @param host
	 *            The host address, as an array of 4 bytes.
	 * @param port
	 *            The port, between 0 and 65535
	 * @param tag
	 *            The tag, between 0 and 7
	 * @param strip
	 *            if the SDP header should be stripped from the packet.
	 * @param useSender
	 *            if the sender's IP address and port should be used.
	 */
	public ClearIOBUF(HasCoreLocation core, boolean expectResponse) {
		super(new RunningSDPHeader(core, expectResponse), CLEAR_IOBUF,
				0, 0, expectResponse ? 1 : 0, null);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new CheckOKResponse("clear iobuf", CLEAR_IOBUF, buffer);
	}
}
