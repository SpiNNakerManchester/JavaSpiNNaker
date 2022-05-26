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
import static uk.ac.manchester.spinnaker.messages.bmp.SerialFlashOp.CRC;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_SF;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** An SCP request to get the CRC of serial flash memory from a BMP. */
public class ReadSerialFlashCRC
		extends BMPRequest<ReadSerialFlashCRC.Response> {
	/**
	 * @param board
	 *            which board's BMP's serial flash should be checked
	 * @param address
	 *            The positive base address to start the check from
	 * @param size
	 *            The number of bytes to check
	 */
	public ReadSerialFlashCRC(BMPBoard board, int address, int size) {
		super(board, CMD_BMP_SF, address, size, CRC.value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request to get the CRC of serial flash.
	 */
	public static class Response extends BMPRequest.BMPResponse {
		/** The CRC. */
		public final int crc;

		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Read Serial Flash CRC", CMD_BMP_SF, buffer);
			buffer = buffer.asReadOnlyBuffer().order(LITTLE_ENDIAN);
			crc = buffer.getInt();
		}
	}
}
