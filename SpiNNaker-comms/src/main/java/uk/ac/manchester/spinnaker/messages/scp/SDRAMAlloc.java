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
package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.String.format;
import static uk.ac.manchester.spinnaker.messages.scp.AllocFree.ALLOC_SDRAM;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE2;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_ALLOC;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.MemoryAllocationFailedException;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to allocate space in the SDRAM space. The response payload is
 * the {@linkplain MemoryLocation location} of the block of allocated memory.
 * <p>
 * Calls {@code cmd_alloc()} (and hence {@code sark_xalloc()}) in
 * {@code scamp-cmd.c}.
 */
public class SDRAMAlloc extends SCPRequest<SDRAMAlloc.Response> {
	private static final int MAX_SDRAM_TAG = 255;

	private static final int FLAG_TAG_RETRY = 4;

	private final int size;

	/**
	 * @param chip
	 *            the chip to allocate on
	 * @param appID
	 *            The ID of the application
	 * @param size
	 *            The size in bytes of memory to be allocated
	 */
	public SDRAMAlloc(HasChipLocation chip, AppID appID, int size) {
		this(chip, appID, size, 0);
	}

	/**
	 * @param chip
	 *            the chip to allocate on
	 * @param appID
	 *            The ID of the application
	 * @param size
	 *            The size in bytes of memory to be allocated
	 * @param tag
	 *            the tag for the SDRAM, a 8-bit (chip-wide) tag that can be
	 *            looked up by a SpiNNaker application to discover the address
	 *            of the allocated block
	 * @throws IllegalArgumentException
	 *             If a bad tag is given.
	 */
	public SDRAMAlloc(HasChipLocation chip, AppID appID, int size, int tag) {
		super(chip.getScampCore(), CMD_ALLOC, argument(appID), size, tag);
		this.size = size;
		if (tag < 0 || tag > MAX_SDRAM_TAG) {
			throw new IllegalArgumentException(
					"The tag parameter needs to be between 0 and "
							+ MAX_SDRAM_TAG);
		}
	}

	// @formatter:off
	/*
	 * [  31-24 |      23-16 |   15-8 | 7-0 ]
	 * [ unused | extra_flag | app_id |  op ]
	 */
	// @formatter:on
	private static int argument(AppID appID) {
		return (FLAG_TAG_RETRY << BYTE2) | (appID.appID << BYTE1)
				| (ALLOC_SDRAM.value << BYTE0);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException,
			MemoryAllocationFailedException {
		return new Response(buffer);
	}

	/** An SCP response to a request to allocate space in SDRAM. */
	protected final class Response extends
			PayloadedResponse<MemoryLocation, MemoryAllocationFailedException> {
		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException,
				MemoryAllocationFailedException {
			super("SDRAM Allocation", CMD_ALLOC, buffer);
		}

		/** @return The base address allocated. */
		@Override
		protected MemoryLocation parse(ByteBuffer buffer)
				throws MemoryAllocationFailedException {
			var baseAddress = new MemoryLocation(buffer.getInt());
			if (baseAddress.isNull()) {
				throw new MemoryAllocationFailedException(
						format("Could not allocate %d bytes of SDRAM", size));
			}
			return baseAddress;
		}
	}
}
