/*
 * Copyright (c) 2018 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_FILL;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to fill a region of memory on a chip with repeated words of
 * data. There is no response payload.
 * <p>
 * Calls {@code sark_cmd_fill()} in {@code sark_base.c}, or {@code cmd_fill()}
 * in {@code bmp_cmd.c} if sent to a BMP.
 */
public final class FillRequest extends SCPRequest<EmptyResponse> {
	/**
	 * @param chip
	 *            The chip to read from
	 * @param baseAddress
	 *            The positive base address to start the fill from. <em>Must be
	 *            word-aligned.</em>
	 * @param data
	 *            The <em>word</em> of data to fill in the space with.
	 * @param size
	 *            The number of <em>bytes</em> to fill in. <em>Must be a
	 *            multiple of 4.</em>
	 */
	public FillRequest(HasChipLocation chip, MemoryLocation baseAddress,
			int data, int size) {
		super(chip.getScampCore(), CMD_FILL, baseAddress.address, data, size);
	}

	@Override
	public EmptyResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new EmptyResponse("Fill", CMD_FILL, buffer);
	}
}
