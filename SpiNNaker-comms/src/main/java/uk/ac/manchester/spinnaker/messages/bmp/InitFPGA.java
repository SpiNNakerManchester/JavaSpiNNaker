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

import static uk.ac.manchester.spinnaker.messages.bmp.XilinxCommand.Init;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_XILINX;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;

/** Start an initialisation of the FPGAs. Terminated by {@link ResetFPGA}. */
public class InitFPGA extends SimpleRequest {
	/**
	 * @param board
	 *            Which board's FPGA to initialise.
	 * @param mask
	 *            Which FPGAs on the board to initialise.
	 */
	public InitFPGA(BMPBoard board, int mask) {
		super("Init FPGA", board, CMD_XILINX, Init.code, mask);
	}
}
