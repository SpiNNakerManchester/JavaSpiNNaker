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

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_FLASH_COPY;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;

/**
 * A request to update flash memory on a BMP. Must have already been prepared
 * with {@link EraseFlash} and {@link WriteFlashBuffer}.
 */
public final class UpdateFlash extends BMPRequest<BMPRequest.BMPResponse> {
	private static final int REAL_FLASH_ADDRESS = 0x10000;

	/**
	 * @param board
	 *            the board with the BMP to write the flash of
	 * @param baseAddress
	 *            The positive base address to start the update at
	 * @param size
	 *            The number of bytes to copy
	 */
	public UpdateFlash(BMPBoard board, MemoryLocation baseAddress, int size) {
		super(board, CMD_FLASH_COPY, REAL_FLASH_ADDRESS, baseAddress.address,
				size);
	}

	@Override
	public BMPRequest.BMPResponse getSCPResponse(ByteBuffer buffer)
			throws Exception {
		return new BMPRequest.BMPResponse("Update flash", CMD_FLASH_COPY,
				buffer);
	}
}
