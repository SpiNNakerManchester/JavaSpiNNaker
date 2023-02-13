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

import static uk.ac.manchester.spinnaker.messages.bmp.SerialFlashOp.WRITE;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_SF;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.limitSlice;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;

/**
 * A request to write to serial flash on a BMP. There is no response payload.
 * <p>
 * Calls {@code sf_write()} in {@code bmp_ssp.c}.
 */
public final class WriteSerialFlash extends BMPRequest<BMPRequest.BMPResponse> {
	/** The size of chunk that will be transferred. Fixed. */
	public static final int FLASH_CHUNK_SIZE = 256;

	/**
	 * @param board
	 *            the board with the BMP to write the flash of
	 * @param baseAddress
	 *            The positive base address where the chunk is located
	 * @param data
	 *            The data to transfer; up to {@link #FLASH_CHUNK_SIZE} bytes.
	 *            The position and limit of the buffer will not be updated by
	 *            this constructor.
	 */
	public WriteSerialFlash(BMPBoard board, MemoryLocation baseAddress,
			ByteBuffer data) {
		super(board, CMD_BMP_SF, baseAddress.address(), FLASH_CHUNK_SIZE,
				WRITE.value, limitSlice(data, FLASH_CHUNK_SIZE));
	}

	@Override
	public BMPRequest.BMPResponse getSCPResponse(ByteBuffer buffer)
			throws Exception {
		return new BMPRequest.BMPResponse("Transfer chunk to flash",
				CMD_BMP_SF, buffer);
	}
}
