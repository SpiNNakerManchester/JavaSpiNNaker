/*
 * Copyright (c) 2018 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LINK_READ;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.FPGA;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * Requests the data from a FPGA's register.
 */
public class ReadFPGARegister extends BMPRequest<ReadFPGARegister.Response> {
	private static final int MASK = 0b00000011;

	/**
	 * @param fpga
	 *            FPGA (0, 1 or 2 on SpiNN-5 board) to communicate with.
	 * @param register
	 *            Register address to read to (will be rounded down to the
	 *            nearest 32-bit word boundary).
	 * @param board
	 *            which board to request the ADC register from
	 * @throws IllegalArgumentException
	 *             If {@link FPGA#FPGA_ALL} is used.
	 */
	public ReadFPGARegister(FPGA fpga, int register, BMPBoard board) {
		super(board, CMD_LINK_READ, register & ~MASK, WORD_SIZE, fpga.value);
		if (!fpga.isSingleFPGA()) {
			throw new IllegalArgumentException(
					"cannot read multiple FPGAs at once with this message");
		}
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for the contents of an FPGA register. */
	public final class Response extends BMPRequest.BMPResponse {
		/** The ADC information. */
		public final int fpgaRegister;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read FPGA register", CMD_LINK_READ, buffer);
			fpgaRegister = buffer.getInt();
		}
	}
}
