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

import static uk.ac.manchester.spinnaker.messages.model.TransferUnit.efficientTransferUnit;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_WRITE;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to write memory on a chip. There is no response payload.
 * <p>
 * Calls {@code sark_cmd_write()} in {@code sark_base.c}.
 */
public class WriteMemory extends SCPRequest<EmptyResponse> {
	/**
	 * @param core
	 *            the core to write via
	 * @param baseAddress
	 *            The positive base address to start the write at
	 * @param data
	 *            Between 1 and 256 bytes to write; the <i>position</i> of the
	 *            buffer must be the point where the data starts. The position
	 *            and limit of the buffer will not be updated by this
	 *            constructor.
	 */
	public WriteMemory(HasCoreLocation core, MemoryLocation baseAddress,
			ByteBuffer data) {
		super(core, CMD_WRITE, baseAddress.address, data.remaining(),
				efficientTransferUnit(baseAddress, data.remaining()).value,
				data);
	}

	/**
	 * @param chip
	 *            the chip to write via
	 * @param baseAddress
	 *            The positive base address to start the read from
	 * @param data
	 *            Between 1 and 256 bytes to write; the <i>position</i> of the
	 *            buffer must be the point where the data starts. The position
	 *            and limit of the buffer will not be updated by this
	 *            constructor.
	 */
	public WriteMemory(HasChipLocation chip, MemoryLocation baseAddress,
			ByteBuffer data) {
		super(chip.getScampCore(), CMD_WRITE, baseAddress.address,
				data.remaining(),
				efficientTransferUnit(baseAddress, data.remaining()).value,
				data);
	}

	@Override
	public EmptyResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new EmptyResponse("Write", CMD_WRITE, buffer);
	}
}
