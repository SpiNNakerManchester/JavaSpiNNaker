/*
 * Copyright (c) 2022 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.bmp.XilinxCommand.Reset;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_XILINX;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.transceiver.BMPTransceiverInterface.FPGAResetType;

/**
 * A request to perform a reset of the FPGAs managed by a BMP. There is no
 * response payload.
 * <p>
 * Calls {@code fpga_reset()} in {@code bmp_hw.c}.
 */
public class ResetFPGA extends BMPRequest<BMPRequest.BMPResponse> {
	/**
	 * @param board
	 *            Which board to reset the FPGAs of.
	 * @param resetType
	 *            What type of reset to do.
	 */
	public ResetFPGA(BMPBoard board, FPGAResetType resetType) {
		super(board, CMD_XILINX, Reset.code, resetType.ordinal());
	}

	@Override
	public BMPResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new BMPResponse("Reset XILINX", CMD_XILINX, buffer);
	}
}
