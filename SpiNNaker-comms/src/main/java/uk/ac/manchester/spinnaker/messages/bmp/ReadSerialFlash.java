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
import static uk.ac.manchester.spinnaker.messages.bmp.SerialFlashOp.READ;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_SF;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP request to read a region of serial flash from a BMP. The response
 * payload is a read-only little-endian {@link ByteBuffer} intended to be read
 * once.
 * <p>
 * Calls {@code sf_read()} in {@code bmp_ssp.c}.
 */
public class ReadSerialFlash extends BMPRequest<ReadSerialFlash.Response> {
	private static int validate(int size) {
		if (size < 1 || size > UDP_MESSAGE_MAX_SIZE) {
			throw new IllegalArgumentException(
					"size must be in range 1 to 256");
		}
		return size;
	}

	/**
	 * @param board
	 *            which board to read the flash of
	 * @param address
	 *            The positive base address to start the read from
	 * @param size
	 *            The number of bytes to read, between 1 and 256
	 */
	public ReadSerialFlash(BMPBoard board, MemoryLocation address, int size) {
		super(board, CMD_BMP_SF, address.address(), validate(size), READ.value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request to read a region of memory on a chip. Note
	 * that it is up to the caller to manage the buffer position of the returned
	 * response if it is to be read from multiple times.
	 */
	protected static final class Response
			extends BMPRequest.PayloadedResponse<ByteBuffer> {
		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Read Serial Flash", CMD_BMP_SF, buffer);
		}

		/** @return The data read, in a little-endian read-only buffer. */
		@Override
		protected ByteBuffer parse(ByteBuffer buffer) {
			return buffer.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		}
	}
}
