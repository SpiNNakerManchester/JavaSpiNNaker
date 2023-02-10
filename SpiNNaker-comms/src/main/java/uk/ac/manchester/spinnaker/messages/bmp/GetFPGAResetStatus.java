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

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.TransferUnit.WORD;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_READ;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * Get the reset status of a board's FPGAs. The response payload is a boolean,
 * {@code true} if the reset bit is asserted for the given FPGA.
 * <p>
 * Calls {@code cmd_read()} in {@code bmp_cmd.c} with special parameters and
 * parses the result.
 */
public class GetFPGAResetStatus
		extends BMPRequest<GetFPGAResetStatus.Response> {
	/** @param board Which board to get the FPGA reset status of. */
	public GetFPGAResetStatus(BMPBoard board) {
		super(board, CMD_READ, IO_PORT_CONTROL_WORD, WORD_SIZE, WORD.value);
	}

	private static final int IO_PORT_CONTROL_WORD = 0x2009c034;

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	private static final int XIL_RST_BIT = 14;

	/** The response to a request to get the FPGA reset status of a board. */
	protected final class Response
			extends BMPRequest.PayloadedResponse<Boolean> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read XIL_RST", CMD_READ, buffer);
		}

		/** @return The reset status of the FPGA. */
		@Override
		protected Boolean parse(ByteBuffer buffer) {
			int ioPortControlWord = buffer.order(LITTLE_ENDIAN).getInt();
			return ((ioPortControlWord >> XIL_RST_BIT) & 1) != 0;
		}
	}
}
