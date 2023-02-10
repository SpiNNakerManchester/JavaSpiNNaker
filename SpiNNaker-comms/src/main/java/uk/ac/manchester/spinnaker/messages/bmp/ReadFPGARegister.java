/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_FPGA_READ;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.FPGA;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request for the data from a FPGA's register. The response payload is the
 * 32-bit integer contents of the register.
 * <p>
 * Calls {@code cmd_fpga_read()} in {@code bmp_cmd.c}.
 */
public class ReadFPGARegister extends BMPRequest<ReadFPGARegister.Response> {
	/**
	 * @param fpga
	 *            FPGA (0, 1 or 2 on SpiNN-5 board) to communicate with.
	 * @param register
	 *            Register address to read to. Must be aligned
	 * @param board
	 *            which board to request the ADC register from
	 * @throws IllegalArgumentException
	 *             If {@link FPGA#FPGA_ALL} is used or the register address is
	 *             not aligned.
	 */
	public ReadFPGARegister(FPGA fpga, MemoryLocation register,
			BMPBoard board) {
		super(board, CMD_FPGA_READ, register.address(), WORD_SIZE, fpga.value);
		if (!register.isAligned()) {
			throw new IllegalArgumentException(
					"FPGA register addresses must be aligned");
		}
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
	protected final class Response
			extends BMPRequest.PayloadedResponse<Integer> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read FPGA register", CMD_FPGA_READ, buffer);
		}

		/** @return The FPGA register contents, not further interpreted. */
		@Override
		protected Integer parse(ByteBuffer buffer) {
			return buffer.getInt();
		}
	}
}
