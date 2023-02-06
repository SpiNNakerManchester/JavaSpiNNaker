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

import static uk.ac.manchester.spinnaker.messages.bmp.SerialFlashOp.WRITE;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_SF;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.limitSlice;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;

/**
 * A request to write to serial flash on a BMP. There is no response payload.
 * <p>
 * Calls {@code sf_write()} in {@code bmp_ssp.c}.
 */
public class WriteSerialFlash extends BMPRequest<BMPRequest.BMPResponse> {
	/** The size of chunk that will be transferred. Fixed. */
	public static final int FLASH_CHUNK_SIZE = 256;

	/**
	 * @param board
	 *            the board with the BMP to write the flash of
	 * @param baseAddress
	 *            The positive base address where the chunk is located
	 * @param data
	 *            The data to transfer; up to {@link #FLASH_CHUNK_SIZE} bytes.
	 *            The position and limit of the buffer will not be updated by
	 *            this constructor.
	 */
	public WriteSerialFlash(BMPBoard board, MemoryLocation baseAddress,
			ByteBuffer data) {
		super(board, CMD_BMP_SF, baseAddress.address, FLASH_CHUNK_SIZE,
				WRITE.value, limitSlice(data, FLASH_CHUNK_SIZE));
	}

	@Override
	public BMPRequest.BMPResponse getSCPResponse(ByteBuffer buffer)
			throws Exception {
		return new BMPRequest.BMPResponse("Transfer chunk to flash",
				CMD_BMP_SF, buffer);
	}
}
