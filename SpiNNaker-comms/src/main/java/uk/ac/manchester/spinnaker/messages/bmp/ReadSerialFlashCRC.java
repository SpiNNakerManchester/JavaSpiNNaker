/*
 * Copyright (c) 2018 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.bmp.SerialFlashOp.CRC;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_SF;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to get the CRC of serial flash memory from a BMP. The response
 * payload is the 32-bit CRC of the given region of flash.
 * <p>
 * Calls {@code sf_crc32()} in {@code bmp_ssp.c}.
 */
public class ReadSerialFlashCRC
		extends BMPRequest<ReadSerialFlashCRC.Response> {
	/**
	 * @param board
	 *            which board's BMP's serial flash should be checked
	 * @param baseAddress
	 *            The positive base address to start the check from
	 * @param size
	 *            The number of bytes to check
	 */
	public ReadSerialFlashCRC(BMPBoard board, MemoryLocation baseAddress,
			int size) {
		super(board, CMD_BMP_SF, baseAddress.address, size, CRC.value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request to get the CRC of serial flash.
	 */
	protected final class Response
			extends BMPRequest.PayloadedResponse<Integer> {
		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Read Serial Flash CRC", CMD_BMP_SF, buffer);
		}

		/** @return The CRC. */
		@Override
		protected Integer parse(ByteBuffer buffer) {
			return buffer.getInt();
		}
	}
}
