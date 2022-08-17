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

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_WRITE;
import static uk.ac.manchester.spinnaker.messages.scp.TransferUnit.efficientTransferUnit;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/** A request to write memory on a chip. */
public class WriteMemory extends SCPRequest<CheckOKResponse> {
	/**
	 * @param core
	 *            the core to write via
	 * @param baseAddress
	 *            The positive base address to start the write at
	 * @param data
	 *            Between 1 and 256 bytes to write; the <i>position</i> of the
	 *            buffer must be the point where the data starts.
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
	 *            buffer must be the point where the data starts.
	 */
	public WriteMemory(HasChipLocation chip, MemoryLocation baseAddress,
			ByteBuffer data) {
		super(chip.getScampCore(), CMD_WRITE, baseAddress.address,
				data.remaining(),
				efficientTransferUnit(baseAddress, data.remaining()).value,
				data);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Write", CMD_WRITE, buffer);
	}
}
