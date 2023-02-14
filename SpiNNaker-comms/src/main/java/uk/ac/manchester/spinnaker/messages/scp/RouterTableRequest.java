/*
 * Copyright (c) 2019-2023 The University of Manchester
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
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.GATHERER_DATA_SPEED_UP;

import java.nio.ByteBuffer;

import com.google.errorprone.annotations.ForOverride;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/**
 * A command message to an extra monitor control core to manipulate the router
 * table.
 */
public abstract sealed class RouterTableRequest
		extends SCPRequest<EmptyResponse>
		permits LoadApplicationRoutes, LoadSystemRoutes, SaveApplicationRoutes {
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
	public final EmptyResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new EmptyResponse(describe(), cmd, buffer);
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
