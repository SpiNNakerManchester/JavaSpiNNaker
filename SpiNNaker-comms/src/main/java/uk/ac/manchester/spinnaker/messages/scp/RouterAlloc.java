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
import static uk.ac.manchester.spinnaker.messages.model.AllocFree.ALLOC_ROUTING;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_ALLOC;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.MemoryAllocationFailedException;

/** An SCP Request to allocate space for routing entries. */
public class RouterAlloc extends SCPRequest<RouterAlloc.Response> {
	private final int numEntries;

	/**
	 * @param chip
	 *            the chip to allocate on
	 * @param appID
	 *            The ID of the application, between 0 and 255
	 * @param numEntries
	 *            The number of entries to allocate
	 *
	 */
	public RouterAlloc(HasChipLocation chip, AppID appID, int numEntries) {
		super(chip.getScampCore(), CMD_ALLOC, argument1(appID), numEntries);
		this.numEntries = numEntries;
	}

	private static int argument1(AppID appID) {
		return (appID.appID() << BYTE1) | (ALLOC_ROUTING.value << BYTE0);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(numEntries, buffer);
	}

	/** An SCP response to a request to allocate router entries. */
	public static class Response extends CheckOKResponse {
		/** The base entry index allocated within the router, or 0 if none. */
		public final int baseIndex;

		Response(int size, ByteBuffer buffer) throws Exception {
			super("Router Allocation", CMD_ALLOC, buffer);
			baseIndex = buffer.getInt();
			if (baseIndex == 0) {
				throw new MemoryAllocationFailedException(
						format("Could not allocate %d router entries", size));
			}
		}
	}
}
