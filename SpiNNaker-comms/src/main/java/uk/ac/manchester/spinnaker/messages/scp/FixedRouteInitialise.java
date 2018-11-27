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

import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_RTR;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/** Sets a fixed route entry. */
public final class FixedRouteInitialise extends SCPRequest<CheckOKResponse> {
	private static int argument1(int appID) {
		return appID << BYTE1;
	}

	/**
	 * @param chip
	 *            The chip to set the route on.
	 * @param entry
	 *            the fixed route entry (converted for writing)
	 * @param appID
	 *            The ID of the application, between 0 and 255
	 */
	public FixedRouteInitialise(HasChipLocation chip, int entry, int appID) {
		super(chip.getScampCore(), CMD_RTR, argument1(appID), entry);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Fixed Route Initialise", CMD_RTR, buffer);
	}
}
