/*
 * Copyright (c) 2018-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.model.TransferUnit.efficientTransferUnit;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_WRITE;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;

/**
 * A request to write memory on a BMP. There is no response payload.
 * <p>
 * Calls {@code cmd_write()} in {@code bmp_cmd.c}.
 */
public class BMPWriteMemory extends BMPRequest<BMPRequest.BMPResponse> {
	/**
	 * @param board
	 *            the board with the BMP to write the memory of
	 * @param baseAddress
	 *            The positive base address to start the write at
	 * @param data
	 *            Between 1 and 256 bytes to write; the <i>position</i> of the
	 *            buffer must be the point where the data starts, and the data
	 *            must extend up to the <i>limit</i>. The position and limit of
	 *            the buffer will not be updated by this constructor.
	 */
	public BMPWriteMemory(BMPBoard board, MemoryLocation baseAddress,
			ByteBuffer data) {
		super(board, CMD_WRITE, baseAddress.address, data.remaining(),
				efficientTransferUnit(baseAddress, data.remaining()).value,
				data);
	}

	@Override
	public BMPRequest.BMPResponse getSCPResponse(ByteBuffer buffer)
			throws Exception {
		return new BMPRequest.BMPResponse("Write", CMD_WRITE, buffer);
	}
}
