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

import static java.lang.String.format;
import static uk.ac.manchester.spinnaker.messages.model.AllocFree.ALLOC_SDRAM;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_ALLOC;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.MemoryAllocationFailedException;

/** An SCP Request to allocate space in the SDRAM space. */
public class SDRAMAlloc extends SCPRequest<SDRAMAlloc.Response> {
	private static final int MAX_SDRAM_TAG = 255;

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
		super(chip.getScampCore(), CMD_ALLOC, argument1(appID), size, tag);
		this.size = size;
		if (tag < 0 || tag > MAX_SDRAM_TAG) {
			throw new IllegalArgumentException(
					"The tag parameter needs to be between 0 and "
							+ MAX_SDRAM_TAG);
		}
	}

	private static int argument1(AppID appID) {
		return (appID.appID << BYTE1) | (ALLOC_SDRAM.value << BYTE0);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(size, buffer);
	}

	/** An SCP response to a request to allocate space in SDRAM. */
	public static class Response extends CheckOKResponse {
		/** The base address allocated. */
		public final int baseAddress;

		Response(int size, ByteBuffer buffer) throws Exception {
			super("SDRAM Allocation", CMD_ALLOC, buffer);
			baseAddress = buffer.getInt();
			if (baseAddress == 0) {
				throw new MemoryAllocationFailedException(
						format("Could not allocate %d bytes of SDRAM", size));
			}
		}
	}
}
