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

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_FLASH_WRITE;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;

/**
 * A request to write memory to flash on a BMP. Must have already been prepared
 * with {@link EraseFlash}.
 */
public class WriteFlashBuffer extends SimpleRequest {
	/** The size of chunk that will be transferred. Fixed. */
	public static final int FLASH_CHUNK_SIZE = 4096;

	/**
	 * @param board
	 *            the board with the BMP to write the flash of
	 * @param baseAddress
	 *            The positive base address where the chunk is located
	 * @param erase
	 *            Whether to erase first
	 */
	public WriteFlashBuffer(BMPBoard board, MemoryLocation baseAddress,
			boolean erase) {
		super("Transfer Chunk to Flash", board, CMD_FLASH_WRITE,
				baseAddress.address, FLASH_CHUNK_SIZE, erase ? 1 : 0);
	}
}
