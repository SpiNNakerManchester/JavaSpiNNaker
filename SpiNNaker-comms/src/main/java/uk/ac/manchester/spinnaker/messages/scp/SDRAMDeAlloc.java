/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.model.AllocFree.FREE_SDRAM_BY_APP_ID;
import static uk.ac.manchester.spinnaker.messages.model.AllocFree.FREE_SDRAM_BY_POINTER;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_ALLOC;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.MemoryAllocationFailedException;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP Request to free space in the SDRAM. The response payload is the number
 * of blocks that were deallocated.
 * <p>
 * Calls {@code cmd_alloc()} (and hence {@code sark_xfree()} or
 * {@code sark_xfree_id()}) in {@code scamp-cmd.c}.
 */
public class SDRAMDeAlloc extends SCPRequest<SDRAMDeAlloc.Response> {
	private final boolean readNumFreedBlocks;

	/**
	 * Free the memory associated with a given application ID.
	 *
	 * @param chip
	 *            The chip to deallocate on.
	 * @param appID
	 *            The ID of the application.
	 */
	public SDRAMDeAlloc(HasChipLocation chip, AppID appID) {
		super(chip.getScampCore(), CMD_ALLOC, argument1(appID));
		readNumFreedBlocks = true;
	}

	/**
	 * Free a block of memory of known size.
	 *
	 * @param chip
	 *            The chip to deallocate on.
	 * @param baseAddress
	 *            The start address in SDRAM of the block that needs to be
	 *            deallocated.
	 */
	public SDRAMDeAlloc(HasChipLocation chip, MemoryLocation baseAddress) {
		super(chip.getScampCore(), CMD_ALLOC, FREE_SDRAM_BY_POINTER.value,
				baseAddress.address);
		readNumFreedBlocks = false;
	}

	// @formatter:off
	/*
	 * [  31-16 |   15-8 | 7-0 ]
	 * [ unused | app_id |  op ]
	 */
	// @formatter:on
	private static int argument1(AppID appID) {
		return (appID.appID << BYTE1) | (FREE_SDRAM_BY_APP_ID.value << BYTE0);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException,
			MemoryAllocationFailedException {
		return new Response(buffer);
	}

	/** An SCP response to a request to deallocate SDRAM. */
	protected final class Response extends
			PayloadedResponse<Integer, MemoryAllocationFailedException> {
		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException,
				MemoryAllocationFailedException {
			super("SDRAM Deallocation", CMD_ALLOC, buffer);
		}

		/**
		 * @return The number of allocated blocks that have been freed from the
		 *         appID given, or one when the direct block of space to
		 *         deallocate was given.
		 */
		@Override
		protected Integer parse(ByteBuffer buffer)
				throws MemoryAllocationFailedException {
			if (!readNumFreedBlocks) {
				return 1;
			}
			int numFreedBlocks = buffer.getInt();
			if (numFreedBlocks == 0) {
				throw new MemoryAllocationFailedException(
						"Could not deallocate SDRAM");
			}
			return numFreedBlocks;
		}
	}
}
