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
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LINK_WRITE;
import static uk.ac.manchester.spinnaker.transceiver.Utils.word;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.FPGA;

/**
 * A request for writing data to a FPGA register.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/spio/blob/master/designs/spinnaker_fpgas/README.md#spi-interface">
 *      spinnaker_fpga design README listing of FPGA registers</a>
 * @see <a href="https://github.com/SpiNNakerManchester/spio/">The SpI/O project
 *      on GitHub</a>
 */
public class WriteFPGARegister extends SimpleRequest {
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
		super("Write FPGA Register", board, CMD_LINK_WRITE, register.address,
				WORD_SIZE, fpga.value, word(value));
		if (!register.isAligned()) {
			throw new IllegalArgumentException(
					"FPGA register addresses must be aligned");
		}
		if (!fpga.isSingleFPGA()) {
			throw new IllegalArgumentException(
					"cannot write multiple FPGAs at once with this message");
		}
	}
}
