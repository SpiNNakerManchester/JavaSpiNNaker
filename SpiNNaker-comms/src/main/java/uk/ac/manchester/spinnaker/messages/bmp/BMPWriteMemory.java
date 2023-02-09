/*
 * Copyright (c) 2018-2022 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.model.TransferUnit.efficientTransferUnit;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_WRITE;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;

/**
 * A request to write memory on a BMP. There is no response payload.
 * <p>
 * Calls {@code cmd_write()} in {@code bmp_cmd.c}.
 */
public class BMPWriteMemory extends BMPRequest<BMPRequest.BMPResponse> {
	/**
	 * @param board
	 *            the board with the BMP to write the memory of
	 * @param baseAddress
	 *            The positive base address to start the write at
	 * @param data
	 *            Between 1 and 256 bytes to write; the <i>position</i> of the
	 *            buffer must be the point where the data starts, and the data
	 *            must extend up to the <i>limit</i>. The position and limit of
	 *            the buffer will not be updated by this constructor.
	 */
	public BMPWriteMemory(BMPBoard board, MemoryLocation baseAddress,
			ByteBuffer data) {
		super(board, CMD_WRITE, baseAddress.address, data.remaining(),
				efficientTransferUnit(baseAddress, data.remaining()).value,
				data);
	}

	@Override
	public BMPRequest.BMPResponse getSCPResponse(ByteBuffer buffer)
			throws Exception {
		return new BMPRequest.BMPResponse("Write", CMD_WRITE, buffer);
	}
}
