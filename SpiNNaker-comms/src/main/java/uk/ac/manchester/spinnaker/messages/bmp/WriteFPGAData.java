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

import static uk.ac.manchester.spinnaker.messages.bmp.XilinxCommand.LoadData;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_XILINX;

import java.nio.ByteBuffer;

/**
 * Upload a chunk of FPGA initialisation data. Must have been set up by
 * {@link InitFPGA}. Upload process will be terminated by {@link ResetFPGA}.
 */
public class WriteFPGAData extends BMPRequest<BMPRequest.BMPResponse> {
	/**
	 * @param board Which board to upload the FPGA data to.
	 * @param data What data to upload.
	 */
	public WriteFPGAData(BMPBoard board, byte[] data) {
		super(board, CMD_XILINX, LoadData.code, data.length, 0,
				ByteBuffer.wrap(data));
	}

	/**
	 * @param board Which board to upload the FPGA data to.
	 * @param data What data to upload.
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
