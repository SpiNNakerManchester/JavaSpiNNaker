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

import static uk.ac.manchester.spinnaker.messages.bmp.XilinxCommand.LoadData;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_XILINX;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;

/**
 * A request to upload a chunk of FPGA initialisation data. Must have been set
 * up by {@link InitFPGA}. The upload process will be terminated by
 * {@link ResetFPGA}. There is no response payload.
 * <p>
 * Calls {@code ssp1_copy()} in {@code bmp_ssp.c}.
 */
public class WriteFPGAData extends BMPRequest<BMPRequest.BMPResponse> {
	/**
	 * @param board
	 *            Which board to upload the FPGA data to.
	 * @param data
	 *            What data to upload.
	 */
	public WriteFPGAData(BMPBoard board, byte[] data) {
		super(board, CMD_XILINX, LoadData.code, data.length, 0,
				ByteBuffer.wrap(data));
	}

	/**
	 * @param board
	 *            Which board to upload the FPGA data to.
	 * @param data
	 *            What data to upload. The position and limit of the buffer will
	 *            not be updated by this method.
	 */
	public WriteFPGAData(BMPBoard board, ByteBuffer data) {
		super(board, CMD_XILINX, LoadData.code, data.remaining(), 0, data);
	}

	@Override
	public BMPResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new BMPResponse("Load data into XILINX buffer", CMD_XILINX,
				buffer);
	}
}
