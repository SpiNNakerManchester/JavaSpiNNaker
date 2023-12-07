/*
 * Copyright (c) 2019 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.COPY_DATA_IN_PORT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_WRITE;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;


import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPLocation;

/**
 * A command message to the Data In port to write multicast data.
 */
public class SendMCDataRequest extends SCPRequest<EmptyResponse> {
	/** Shift of x in the coordinates for arg2. */
	private static final int X_SHIFT = 16;

	/**
	 * @param core
	 *            Where to send the request.
	 * @param targetCore
	 *            The target core of the write.
	 * @param baseAddress
	 *            The address to write to on the target core.
	 * @param data
	 *            The data to write.
	 */
	public SendMCDataRequest(HasCoreLocation core, HasCoreLocation targetCore,
			MemoryLocation baseAddress, ByteBuffer data) {
		super(header(core), CMD_WRITE, baseAddress.address,
				(targetCore.getX() << X_SHIFT) | targetCore.getY(),
				data.remaining() / WORD_SIZE, data);
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
		return new SDPHeader(REPLY_EXPECTED, new SDPLocation(core),
				COPY_DATA_IN_PORT);
	}

	@Override
	public EmptyResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new EmptyResponse("Copy Data In", CMD_WRITE, buffer);
	}
}
