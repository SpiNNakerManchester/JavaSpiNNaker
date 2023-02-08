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

import static uk.ac.manchester.spinnaker.messages.model.RouterCommand.ROUTER_FIXED;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_RTR;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.RoutingEntry;
import uk.ac.manchester.spinnaker.messages.model.AppID;

/**
 * Sets a fixed route entry. There is no response payload.
 * <p>
 * Calls {@code rtr_fr_set()} in {@code sark_hw.c}, via {@code rtr_cmd()} in
 * {@code scamp-cmd.c}.
 */
public final class FixedRouteInitialise extends SCPRequest<CheckOKResponse> {
	private static int argument1(AppID appID) {
		return (appID.appID << BYTE1) | (ROUTER_FIXED.value << BYTE0);
	}

	/**
	 * @param chip
	 *            The chip to set the route on.
	 * @param entry
	 *            The fixed route entry (converted for writing)
	 * @param appID
	 *            The ID of the application
	 */
	public FixedRouteInitialise(HasChipLocation chip, int entry, AppID appID) {
		super(chip.getScampCore(), CMD_RTR, argument1(appID), entry);
	}

	/**
	 * @param chip
	 *            The chip to set the route on.
	 * @param entry
	 *            The fixed route entry
	 * @param appID
	 *            The ID of the application
	 */
	public FixedRouteInitialise(HasChipLocation chip, RoutingEntry entry,
			AppID appID) {
		this(chip.getScampCore(), entry.encode(), appID);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Fixed Route Initialise", CMD_RTR, buffer);
	}
}
