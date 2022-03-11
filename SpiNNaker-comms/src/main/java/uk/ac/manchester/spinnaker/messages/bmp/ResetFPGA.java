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

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_XILINX;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.transceiver.BMPTransceiverInterface.FPGAResetType;

/** Perform a reset of the FPGAs. */
public class ResetFPGA extends BMPRequest<ResetFPGA.Response> {
	public ResetFPGA(int board, FPGAResetType resetType) {
		// FIXME factor out the 2
		super(board, CMD_XILINX, 2, resetType.ordinal());
	}

	public final class Response extends BMPRequest.BMPResponse {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Reset XILINX", CMD_XILINX, buffer);
		}
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}
}
