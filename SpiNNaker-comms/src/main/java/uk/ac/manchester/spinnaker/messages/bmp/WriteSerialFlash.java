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

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;

/**
 * A request to write to serial flash on a BMP.
 */
public class WriteSerialFlash extends SimpleRequest {
	/** The size of chunk that will be transferred. Fixed. */
	public static final int FLASH_CHUNK_SIZE = 256;

	/**
	 * @param board
	 *            the board with the BMP to write the flash of
	 * @param baseAddress
	 *            The positive base address where the chunk is located
	 * @param data
	 *            The data to transfer; up to {@link #FLASH_CHUNK_SIZE} bytes.
	 *            This does <em>not</em> update the buffer position.
	 */
	public WriteSerialFlash(BMPBoard board, MemoryLocation baseAddress,
			ByteBuffer data) {
		super("Transfer Chunk to Serial Flash", board, CMD_BMP_SF,
				baseAddress.address, FLASH_CHUNK_SIZE, WRITE.value,
				condition(data));
	}

	private static ByteBuffer condition(ByteBuffer data) {
		// Just in case
		if (data.remaining() > FLASH_CHUNK_SIZE) {
			var b = data.duplicate();
			b.limit(b.position() + FLASH_CHUNK_SIZE);
			return b;
		}
		return data;
	}
}
