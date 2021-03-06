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

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_FILL;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/** An SCP request to fill a region of memory on a chip with repeated data. */
public final class FillRequest extends SCPRequest<CheckOKResponse> {
	/**
	 * @param chip
	 *            The chip to read from
	 * @param baseAddress
	 *            The positive base address to start the fill from
	 * @param data
	 *            The data to fill in the space with
	 * @param size
	 *            The number of bytes to fill in
	 */
	public FillRequest(HasChipLocation chip, int baseAddress, int data,
			int size) {
		super(chip.getScampCore(), CMD_FILL, baseAddress, data, size);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Fill", CMD_FILL, buffer);
	}
}
