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

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.TransferUnit.efficientTransferUnit;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_READ;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to read a region of memory from a BMP. The response payload is
 * a read-only little-endian {@link ByteBuffer} intended to be read once.
 * <p>
 * Calls {@code cmd_read()} in {@code bmp_cmd.c}.
 */
public class BMPReadMemory extends BMPRequest<BMPReadMemory.Response> {
	private static int validate(int size) {
		if (size < 1 || size > UDP_MESSAGE_MAX_SIZE) {
			throw new IllegalArgumentException(
					"size must be in range 1 to 256");
		}
		return size;
	}

	/**
	 * @param board
	 *            which board to read the memory of
	 * @param address
	 *            The positive base address to start the read from
	 * @param size
	 *            The number of bytes to read, between 1 and 256
	 */
	public BMPReadMemory(BMPBoard board, MemoryLocation address, int size) {
		super(board, CMD_READ, address.address, validate(size),
				efficientTransferUnit(address, size).value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request to read a region of memory on a chip. Note
	 * that it is up to the caller to manage the buffer position of the returned
	 * response if it is to be read from multiple times.
	 */
	protected final class Response
			extends BMPRequest.PayloadedResponse<ByteBuffer> {
		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Read", CMD_READ, buffer);
		}

		/** @return The data read, in a little-endian read-only buffer. */
		@Override
		protected ByteBuffer parse(ByteBuffer buffer) {
			return buffer.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		}
	}
}
