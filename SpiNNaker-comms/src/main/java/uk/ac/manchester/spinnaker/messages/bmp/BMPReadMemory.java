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

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_READ;
import static uk.ac.manchester.spinnaker.messages.scp.TransferUnit.efficientTransferUnit;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;

/** An SCP request to read a region of memory from a BMP. */
public class BMPReadMemory extends BMPRequest<BMPReadMemory.Response> {
	private static int validate(int size) {
		if (size < 1 || size > UDP_MESSAGE_MAX_SIZE) {
			throw new IllegalArgumentException(
					"size must be in range 1 to 256");
		}
		return size;
	}

	/**
	 * @param board
	 *            which board to read the memory of
	 * @param address
	 *            The positive base address to start the read from
	 * @param size
	 *            The number of bytes to read, between 1 and 256
	 */
	public BMPReadMemory(BMPBoard board, MemoryLocation address, int size) {
		super("Read BMP Memory", board, CMD_READ, address.address,
				validate(size), efficientTransferUnit(address, size).value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request to read a region of memory on a chip.
	 */
	public final class Response
			extends BMPRequest<BMPReadMemory.Response>.BMPResponse {
		/** The data read, in a little-endian read-only buffer. */
		public final ByteBuffer data;

		private Response(ByteBuffer buffer) throws Exception {
			super(buffer);
			this.data = buffer.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		}
	}
}
