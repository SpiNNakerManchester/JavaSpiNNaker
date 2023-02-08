/*
 * Copyright (c) 2018 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LINK_WRITE;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/**
 * A request to write memory on a neighbouring chip. There is no response
 * payload.
 * <p>
 * Calls {@code cmd_link_write()} in {@code scamp-cmd.c}.
 */
public class WriteLink extends SCPRequest<CheckOKResponse> {
	/**
	 * @param core
	 *            the core to write via
	 * @param link
	 *            The ID of the link down which to send the write
	 * @param baseAddress
	 *            The positive base address to start the write at
	 * @param data
	 *            The data to write (up to 256 bytes); the <i>position</i> of
	 *            the buffer must be the point where the data starts. The
	 *            position and limit of the buffer will not be updated by this
	 *            constructor.
	 */
	public WriteLink(HasCoreLocation core, Direction link,
			MemoryLocation baseAddress, ByteBuffer data) {
		super(core, CMD_LINK_WRITE, baseAddress.address, data.remaining(),
				link.id, data);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Write Memory", CMD_LINK_WRITE, buffer);
	}
}
