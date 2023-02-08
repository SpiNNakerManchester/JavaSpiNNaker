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

import static uk.ac.manchester.spinnaker.messages.model.RouterCommand.ROUTER_LOAD;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.HALF1;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_RTR;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.model.AppID;

/**
 * A request to initialise the router on a chip. There is no response payload.
 * <p>
 * Ultimately handled by {@code rtr_mc_load()} in {@code sark_hw.c} (via
 * {@code cmd_rtr()} in {@code scamp-cmd.c}).
 */
public class RouterInit extends SCPRequest<CheckOKResponse> {
	/** One reserved for SCAMP. */
	private static final int MAX_ENTRIES = 1023;

	/**
	 * @param chip
	 *            The coordinates of the chip to clear the router of
	 * @param numEntries
	 *            The number of entries in the table (more than 0)
	 * @param tableAddress
	 *            The allocated address containing the data to init the router
	 *            from.
	 * @param baseIndex
	 *            The base index in the router table where the entries are to be
	 *            placed.
	 * @param appID
	 *            The ID of the application with which to associate the routes.
	 * @throws IllegalArgumentException
	 *             If a bad address or entry count is given
	 */
	public RouterInit(HasChipLocation chip, int numEntries,
			MemoryLocation tableAddress, int baseIndex,
			AppID appID) {
		super(chip.getScampCore(), CMD_RTR, argument1(numEntries, appID),
				tableAddress.address, baseIndex);
		if (baseIndex < 0) {
			throw new IllegalArgumentException(
					"baseIndex must not be negative");
		}
	}

	private static int argument1(int numEntries, AppID appID) {
		if (numEntries < 1) {
			throw new IllegalArgumentException(
					"numEntries must be more than 0");
		} else if (numEntries > MAX_ENTRIES) {
			throw new IllegalArgumentException(
					"numEntries must be no more than " + MAX_ENTRIES);
		}
		return (numEntries << HALF1) | (appID.appID << BYTE1)
				| (ROUTER_LOAD.value << BYTE0);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Router Init", CMD_RTR, buffer);
	}
}
