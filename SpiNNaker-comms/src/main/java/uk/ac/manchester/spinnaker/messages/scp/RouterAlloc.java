/*
 * Copyright (c) 2018 The University of Manchester
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

import static java.lang.String.format;
import static uk.ac.manchester.spinnaker.messages.scp.AllocFree.ALLOC_ROUTING;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_ALLOC;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.MemoryAllocationFailedException;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to allocate space for routing entries. The response payload is
 * the base index of the allocated block of routing entries.
 * <p>
 * Calls {@code rtr_alloc_id()} in {@code sark_alloc.c} via {@code cmd_alloc()}
 * in {@code scamp-cmd.c}.
 */
public final class RouterAlloc extends SCPRequest<RouterAlloc.Response> {
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
	public Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException,
			MemoryAllocationFailedException {
		return new Response(buffer);
	}

	/** An SCP response to a request to allocate router entries. */
	protected final class Response extends
			PayloadedResponse<Integer, MemoryAllocationFailedException> {
		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException,
				MemoryAllocationFailedException {
			super("Router Allocation", CMD_ALLOC, buffer);
		}

		/** @return The base entry index allocated within the router. */
		@Override
		protected Integer parse(ByteBuffer buffer)
				throws MemoryAllocationFailedException {
			int baseIndex = buffer.getInt();
			if (baseIndex == 0) {
				throw new MemoryAllocationFailedException(format(
						"Could not allocate %d router entries", numEntries));
			}
			return baseIndex;
		}
	}
}
