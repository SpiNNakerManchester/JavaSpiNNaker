/*
 * Copyright (c) 2022 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
