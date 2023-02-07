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
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.EXTRA_MONITOR_CORE_REINJECTION;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/**
 * A command message to an extra monitor control core to manipulate the packet
 * reinjection subsystem.
 *
 * @param <T>
 *            The type of response.
 */
//TODO Seal in 17
public abstract class ReinjectorRequest<T extends CheckOKResponse>
		extends SCPRequest<T> {
	/**
	 * @param core
	 *            Where to send the request.
	 * @param command
	 *            What command we are invoking.
	 */
	ReinjectorRequest(HasCoreLocation core, ReinjectorCommand command) {
		super(header(core), command, 0, 0, 0, NO_DATA);
	}

	/**
	 * @param core
	 *            Where to send the request.
	 * @param command
	 *            What command we are invoking.
	 * @param argument
	 *            What argument to provide (encoded as an integer).
	 */
	ReinjectorRequest(HasCoreLocation core, ReinjectorCommand command,
			int argument) {
		super(header(core), command, argument, 0, 0, NO_DATA);
	}

	/**
	 * @param core
	 *            Where to send the request.
	 * @param command
	 *            What command we are invoking.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 * @param argument3
	 *            The third argument.
	 * @param body
	 *            Subsequent binary data.
	 */
	ReinjectorRequest(HasCoreLocation core, ReinjectorCommand command,
			int argument1, int argument2, int argument3, ByteBuffer body) {
		super(header(core), command, argument1, argument2, argument3, body);
	}

	/**
	 * Make a variant of SDP header that talks to the packet reinjector. It
	 * <i>always</i> wants a reply and always talks to a particular SDP port
	 * (the port for the reinjector).
	 *
	 * @param core
	 *            The SpiNNaker core that we want to talk to. Should be running
	 *            the extra monitor core (not checked).
	 * @return The SDP header.
	 */
	private static SDPHeader header(HasCoreLocation core) {
		return new SDPHeader(REPLY_EXPECTED, core,
				EXTRA_MONITOR_CORE_REINJECTION);
	}
}
