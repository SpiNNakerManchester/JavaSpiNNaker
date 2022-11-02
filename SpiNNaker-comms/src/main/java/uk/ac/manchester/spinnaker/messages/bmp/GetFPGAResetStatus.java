/*
 * Copyright (c) 2022 The University of Manchester
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
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_READ;
import static uk.ac.manchester.spinnaker.messages.scp.TransferUnit.WORD;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** Get the reset status of a board's FPGAs. */
public class GetFPGAResetStatus
		extends BMPRequest<GetFPGAResetStatus.Response> {
	private static final int IO_PORT_CONTROL_WORD = 0x2009c034;

	private static final int XIL_RST_BIT = 14;

	/**
	 * @param board
	 *            Which board to get the FPGA reset status of.
	 */
	public GetFPGAResetStatus(BMPBoard board) {
		super("Read FPGA Reset Status", board, CMD_READ, IO_PORT_CONTROL_WORD,
				WORD_SIZE, WORD.value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** The response to a request to get the FPGA reset status of a board. */
	public final class Response
			extends BMPRequest<GetFPGAResetStatus.Response>.BMPResponse {
		private int ioPortControlWord;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super(buffer);
			buffer.order(LITTLE_ENDIAN);
			ioPortControlWord = buffer.getInt();
		}

		/** @return The reset status of the FPGA. */
		public boolean isReset() {
			return ((ioPortControlWord >> XIL_RST_BIT) & 1) != 0;
		}
	}
}
