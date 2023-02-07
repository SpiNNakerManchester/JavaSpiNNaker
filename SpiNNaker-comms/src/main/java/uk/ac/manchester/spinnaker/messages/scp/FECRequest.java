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

import static uk.ac.manchester.spinnaker.messages.Utils.wordAsBuffer;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_NOT_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.RUNNING_COMMAND_SDP_PORT;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/**
 * A command message to a core using FrontEndCommon lib's simulation management
 * subsystem.
 *
 * @param <T>
 *            The type of response expected.
 */
// TODO Seal in 17
public abstract class FECRequest<T extends SCPResponse> extends SCPRequest<T> {
	/**
	 * @param core
	 *            Where to send the request.
	 * @param replyExpected
	 *            Whether we expect a reply.
	 * @param command
	 *            What command we are invoking.
	 */
	FECRequest(HasCoreLocation core, boolean replyExpected,
			RunningCommand command) {
		super(header(core, replyExpected), command, 0, 0, 0, NO_DATA);
	}

	/**
	 * @param core
	 *            Where to send the request.
	 * @param replyExpected
	 *            Whether we expect a reply.
	 * @param command
	 *            What command we are invoking.
	 * @param arg1
	 *            Argument 1.
	 * @param arg2
	 *            Argument 2.
	 * @param arg3
	 *            Argument 3.
	 * @param arg4
	 *            Argument 4.
	 */
	FECRequest(HasCoreLocation core, boolean replyExpected,
			RunningCommand command, int arg1, int arg2, int arg3, int arg4) {
		super(header(core, replyExpected), command, arg1, arg2, arg3,
				wordAsBuffer(arg4));
	}

	/**
	 * Make a variant of SDP header that is used to talk to running cores.
	 *
	 * @param core
	 *            The SpiNNaker core that we want to talk to.
	 * @param replyExpected
	 *            Whether we expect a reply.
	 * @return The SDP header.
	 */
	private static SDPHeader header(HasCoreLocation core,
			boolean replyExpected) {
		return new SDPHeader(
				replyExpected ? REPLY_EXPECTED : REPLY_NOT_EXPECTED, core,
				RUNNING_COMMAND_SDP_PORT);
	}
}
