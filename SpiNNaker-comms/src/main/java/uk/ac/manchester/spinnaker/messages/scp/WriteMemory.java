/*
 * Copyright (c) 2018 The University of Manchester
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
