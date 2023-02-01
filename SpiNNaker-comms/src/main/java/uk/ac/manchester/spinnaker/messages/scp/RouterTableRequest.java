/*
 * Copyright (c) 2019-2023 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.GATHERER_DATA_SPEED_UP;

import java.nio.ByteBuffer;

import com.google.errorprone.annotations.ForOverride;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/**
 * A command message to an extra monitor control core to manipulate the router
 * table.
 */
//TODO Seal in 17
public abstract class RouterTableRequest extends SCPRequest<CheckOKResponse> {
	private final RouterTableCommand cmd;

	/**
	 * @param core
	 *            Where to send the request.
	 * @param command
	 *            What command we are invoking.
	 */
	RouterTableRequest(HasCoreLocation core, RouterTableCommand command) {
		super(header(core), command, 0, 0, 0, NO_DATA);
		cmd = command;
	}

	/**
	 * Describe the requested operation.
	 *
	 * @return A human-readable description of the operation, for failure
	 *         reporting.
	 */
	@ForOverride
	abstract String describe();

	@Override
	public final CheckOKResponse getSCPResponse(ByteBuffer buffer)
			throws Exception {
		return new CheckOKResponse(describe(), cmd, buffer);
	}

	/**
	 * Make a variant of SDP that talks to the packet reinjector for doing
	 * saving and loading of multicast router tables.
	 *
	 * @param core
	 *            The SpiNNaker core that we want to talk to. Should be running
	 *            the extra monitor core.
	 * @return The SDP header.
	 */
	static final SDPHeader header(HasCoreLocation core) {
		return new SDPHeader(REPLY_EXPECTED, core, GATHERER_DATA_SPEED_UP);
	}
}
