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

import static uk.ac.manchester.spinnaker.messages.bmp.WriteFlashBuffer.FLASH_CHUNK_SIZE;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_FLASH_ERASE;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** A request to erase flash memory on a BMP. */
public final class EraseFlash extends BMPRequest<EraseFlash.Response> {
	private static final int LOW_BOUNDARY = 65536;

	private static final int MEMORY_LIMIT = 524288;

	/**
	 * @param board
	 *            the board with the BMP to write the memory of
	 * @param baseAddress
	 *            The positive base address to start the erase at
	 * @param size
	 *            The number of bytes to erase
	 * @throws IllegalArgumentException
	 *             If the baseAddress or size make no sense
	 */
	public EraseFlash(BMPBoard board, MemoryLocation baseAddress, int size) {
		super(board, CMD_FLASH_ERASE, baseAddress.address,
				baseAddress.address + size);
		// Check that we've been actually asked to do something sane!
		if (size <= 0) {
			throw new IllegalArgumentException("no data");
		}
		int addr = baseAddress.address;
		if (addr < 0 || addr + size > MEMORY_LIMIT || addr + size < 0) {
			throw new IllegalArgumentException("address not in flash");
		}
		if ((addr % FLASH_CHUNK_SIZE) != 0) {
			throw new IllegalArgumentException("not on 4kB boundary");
		}
		if (addr < LOW_BOUNDARY && addr + size > LOW_BOUNDARY) {
			throw new IllegalArgumentException("crosses flash 4k/32k boundary");
		}
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** The response from a request to erase flash. */
	public static final class Response extends BMPRequest.BMPResponse {
		/** Where the buffer is located. */
		public final MemoryLocation address;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Erase flash memory", CMD_FLASH_ERASE, buffer);
			address = new MemoryLocation(buffer.getInt());
		}
	}
}
