/*
 * Copyright (c) 2019 The University of Manchester
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
