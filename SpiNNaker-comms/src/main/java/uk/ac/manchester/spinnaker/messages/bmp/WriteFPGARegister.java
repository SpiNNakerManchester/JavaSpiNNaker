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
import static uk.ac.manchester.spinnaker.messages.Utils.wordAsBuffer;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_FPGA_WRITE;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.FPGA;

/**
 * A request to write data to an FPGA register managed by a BMP. There is no
 * response payload.
 * <p>
 * Calls {@code cmd_fpga_write()} in {@code bmp_cmd.c}.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/spio/blob/master/designs/spinnaker_fpgas/README.md#spi-interface">
 *      spinnaker_fpga design README listing of FPGA registers</a>
 * @see <a href="https://github.com/SpiNNakerManchester/spio/">The SpI/O project
 *      on GitHub</a>
 */
public class WriteFPGARegister extends BMPRequest<BMPRequest.BMPResponse> {
	/**
	 * @param fpga
	 *            FPGA (0, 1 or 2 on SpiNN-5 board) to communicate with.
	 * @param register
	 *            Register address to read to. Must be aligned.
	 * @param value
	 *            A 32-bit value to write to the register.
	 * @param board
	 *            which board to write the ADC register on
	 * @throws IllegalArgumentException
	 *             If {@link FPGA#FPGA_ALL} is used or the register address is
	 *             not aligned.
	 */
	public WriteFPGARegister(FPGA fpga, MemoryLocation register, int value,
			BMPBoard board) {
		super(board, CMD_FPGA_WRITE, register.address, WORD_SIZE, fpga.value,
				wordAsBuffer(value));
		if (!register.isAligned()) {
			throw new IllegalArgumentException(
					"FPGA register addresses must be aligned");
		}
		if (!fpga.isSingleFPGA()) {
			throw new IllegalArgumentException(
					"cannot write multiple FPGAs at once with this message");
		}
	}

	@Override
	public BMPResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new BMPResponse("Send FPGA register write", CMD_FPGA_WRITE,
				buffer);
	}
}
