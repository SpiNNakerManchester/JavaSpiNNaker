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

import static uk.ac.manchester.spinnaker.messages.bmp.SerialFlashOp.CRC;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_SF;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP request to get the CRC of serial flash memory from a BMP.
 * <p>
 * Calls {@code sf_crc32()} in {@code bmp_ssp.c}.
 */
public class ReadSerialFlashCRC
		extends BMPRequest<ReadSerialFlashCRC.Response> {
	/**
	 * @param board
	 *            which board's BMP's serial flash should be checked
	 * @param baseAddress
	 *            The positive base address to start the check from
	 * @param size
	 *            The number of bytes to check
	 */
	public ReadSerialFlashCRC(BMPBoard board, MemoryLocation baseAddress,
			int size) {
		super(board, CMD_BMP_SF, baseAddress.address, size, CRC.value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request to get the CRC of serial flash.
	 */
	public static final class Response
			extends BMPRequest.PayloadedResponse<Integer> {
		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Read Serial Flash CRC", CMD_BMP_SF, buffer);
		}

		/** @return The CRC. */
		@Override
		protected Integer parse(ByteBuffer buffer) {
			return buffer.getInt();
		}
	}
}
