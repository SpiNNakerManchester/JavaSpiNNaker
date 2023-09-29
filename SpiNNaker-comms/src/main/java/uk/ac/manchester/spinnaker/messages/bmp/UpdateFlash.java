/*
 * Copyright (c) 2018 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_FLASH_COPY;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;

/**
 * A request to update flash memory on a BMP. Must have already been prepared
 * with {@link EraseFlash} and {@link WriteFlashBuffer}. There is no response
 * payload.
 * <p>
 * This calls {@code flash_copy()} in {@code bmp_boot.c}.
 */
public final class UpdateFlash extends BMPRequest<BMPRequest.BMPResponse> {
	private static final int REAL_FLASH_ADDRESS = 0x10000;

	/**
	 * @param board
	 *            the board with the BMP to write the flash of
	 * @param baseAddress
	 *            The positive base address to start the update at
	 * @param size
	 *            The number of bytes to copy
	 */
	public UpdateFlash(BMPBoard board, MemoryLocation baseAddress, int size) {
		super(board, CMD_FLASH_COPY, REAL_FLASH_ADDRESS, baseAddress.address(),
				size);
	}

	@Override
	public BMPRequest.BMPResponse getSCPResponse(ByteBuffer buffer)
			throws Exception {
		return new BMPRequest.BMPResponse("Update flash", CMD_FLASH_COPY,
				buffer);
	}
}
