/*
 * Copyright (c) 2018-2023 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.bmp.XilinxCommand.Init;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_XILINX;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;

/**
 * A request to start an initialisation of the FPGAs. Terminated by
 * {@link ResetFPGA}. There is no response payload.
 * <p>
 * Calls {@code fpga_init()} in {@code bmp_hw.c}.
 */
public class InitFPGA extends BMPRequest<BMPRequest.BMPResponse> {
	/**
	 * @param board
	 *            Which board's FPGA to initialise.
	 * @param mask
	 *            Which FPGAs on the board to initialise.
	 */
	public InitFPGA(BMPBoard board, int mask) {
		super(board, CMD_XILINX, Init.code, mask);
	}

	@Override
	public BMPResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new BMPResponse("Init XILINX", CMD_XILINX, buffer);
	}
}
