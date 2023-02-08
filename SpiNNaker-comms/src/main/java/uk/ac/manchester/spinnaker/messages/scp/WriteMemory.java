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

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_WRITE;
import static uk.ac.manchester.spinnaker.messages.scp.TransferUnit.efficientTransferUnit;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/**
 * A request to write memory on a chip. There is no response payload.
 * <p>
 * Calls {@code sark_cmd_write()} in {@code sark_base.c}.
 */
public class WriteMemory extends SCPRequest<CheckOKResponse> {
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
		super(core, CMD_WRITE, baseAddress.address(), data.remaining(),
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
		super(chip.getScampCore(), CMD_WRITE, baseAddress.address(),
				data.remaining(),
				efficientTransferUnit(baseAddress, data.remaining()).value,
				data);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Write", CMD_WRITE, buffer);
	}
}
