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
package uk.ac.manchester.spinnaker.messages.scp;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LINK_READ;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to read a region of memory via a link on a chip. The response
 * payload is a read-only little-endian {@link ByteBuffer} intended to be read
 * once.
 * <p>
 * Calls {@code cmd_link_read()} in {@code scamp-cmd.c}.
 */
public class ReadLink extends SCPRequest<ReadLink.Response> {
	private static int validate(int size) {
		if (size < 1 || size > UDP_MESSAGE_MAX_SIZE) {
			throw new IllegalArgumentException(
					"size must be in range 1 to 256");
		}
		return size;
	}

	/**
	 * @param core
	 *            the core to read via
	 * @param link
	 *            The direction of the link down which to send the query
	 * @param baseAddress
	 *            The positive base address to start the read from
	 * @param size
	 *            The number of bytes to read, between 1 and 256
	 */
	public ReadLink(HasCoreLocation core, Direction link,
			MemoryLocation baseAddress, int size) {
		super(core, CMD_LINK_READ, baseAddress.address, validate(size),
				link.id);
	}

	/**
	 * @param chip
	 *            the chip to read via
	 * @param link
	 *            The direction of the link down which to send the query
	 * @param baseAddress
	 *            The positive base address to start the read from
	 * @param size
	 *            The number of bytes to read, between 1 and 256
	 */
	public ReadLink(HasChipLocation chip, Direction link,
			MemoryLocation baseAddress, int size) {
		super(chip.getScampCore(), CMD_LINK_READ, baseAddress.address,
				validate(size), link.id);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request to read a region of memory via a link on a
	 * chip. Note that it is up to the caller to manage the buffer position of
	 * the returned response if it is to be read from multiple times.
	 */
	protected final class Response
			extends PayloadedResponse<ByteBuffer, RuntimeException> {
		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Read Link", CMD_LINK_READ, buffer);
		}

		/** @return The data read, as a read-only little-endian buffer. */
		@Override
		protected ByteBuffer parse(ByteBuffer buffer) {
			return buffer.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		}
	}
}
