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

import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE2;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_RTR;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.AppID;

/** A request to initialise the router on a chip. */
public class RouterInit extends SCPRequest<CheckOKResponse> {
	/**
	 * @param chip
	 *            The coordinates of the chip to clear the router of
	 * @param numEntries
	 *            The number of entries in the table (more than 0)
	 * @param tableAddress
	 *            The allocated table address
	 * @param baseAddress
	 *            The base address containing the entries
	 * @param appID
	 *            The ID of the application with which to associate the routes.
	 */
	public RouterInit(HasChipLocation chip, int numEntries, int tableAddress,
			int baseAddress, AppID appID) {
		super(chip.getScampCore(), CMD_RTR, argument1(numEntries, appID),
				tableAddress, baseAddress);
		if (numEntries < 1) {
			throw new IllegalArgumentException(
					"numEntries must be more than 0");
		}
		if (baseAddress < 0) {
			throw new IllegalArgumentException(
					"baseAddress must not be negative");
		}
		if (tableAddress < 0) {
			throw new IllegalArgumentException(
					"tableAddress must not be negative");
		}
	}

	private static int argument1(int numEntries, AppID appID) {
		return (numEntries << BYTE2) | (appID.appID << BYTE1) | (2 << BYTE0);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Router Init", CMD_RTR, buffer);
	}
}
