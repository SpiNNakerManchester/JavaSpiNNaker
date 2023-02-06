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

import uk.ac.manchester.spinnaker.messages.scp.SCPCommand;
import uk.ac.manchester.spinnaker.transceiver.BMPTransceiverInterface.FPGAResetType;

import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Commands to send to the Xilinx FPGA handler. Used as the first argument of
 * {@link SCPCommand#CMD_XILINX}. (Unnamed in the BMP source code.)
 */
@UsedInJavadocOnly({ FPGAResetType.class, SCPCommand.class })
enum XilinxCommand {
	/** Load block of data. */
	LoadData(0),
	/** Start initialisation. */
	Init(1),
	/**
	 * Reset.
	 *
	 * @see FPGAResetType
	 */
	Reset(2);

	/** The command code to use in the message. */
	public final int code;

	XilinxCommand(int code) {
		this.code = code;
	}
}
