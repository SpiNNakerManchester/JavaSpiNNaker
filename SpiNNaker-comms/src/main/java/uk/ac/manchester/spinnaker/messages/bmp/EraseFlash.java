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

import static uk.ac.manchester.spinnaker.messages.bmp.WriteFlashBuffer.FLASH_CHUNK_SIZE;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_FLASH_ERASE;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to erase flash memory on a BMP. The response payload is the
 * {@linkplain MemoryLocation location} of the flash buffer.
 * <p>
 * Handled by {@code cmd_flash_erase()} in {@code bmp_cmd.c}.
 */
public final class EraseFlash extends BMPRequest<EraseFlash.Response> {
	private static final int LOW_BOUNDARY = 65536;

	private static final int MEMORY_LIMIT = 524288;

	/**
	 * @param board
	 *            the board with the BMP to write the memory of
	 * @param baseAddress
	 *            The positive base address to start the erase at
	 * @param size
	 *            The number of bytes to erase
	 * @throws IllegalArgumentException
	 *             If the baseAddress or size make no sense
	 */
	public EraseFlash(BMPBoard board, MemoryLocation baseAddress, int size) {
		super(board, CMD_FLASH_ERASE, baseAddress.address(),
				baseAddress.address() + size);
		// Check that we've been actually asked to do something sane!
		if (size <= 0) {
			throw new IllegalArgumentException("no data");
		}
		int addr = baseAddress.address();
		if (addr < 0 || addr + size > MEMORY_LIMIT || addr + size < 0) {
			throw new IllegalArgumentException("address not in flash");
		}
		if ((addr % FLASH_CHUNK_SIZE) != 0) {
			throw new IllegalArgumentException("not on 4kB boundary");
		}
		if (addr < LOW_BOUNDARY && addr + size > LOW_BOUNDARY) {
			throw new IllegalArgumentException("crosses flash 4k/32k boundary");
		}
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** The response from a request to erase flash. */
	protected final class Response
			extends BMPRequest.PayloadedResponse<MemoryLocation> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Erase flash memory", CMD_FLASH_ERASE, buffer);
		}

		/** @return Where the buffer is located. */
		@Override
		protected MemoryLocation parse(ByteBuffer buffer) {
			return new MemoryLocation(buffer.getInt());
		}
	}
}
