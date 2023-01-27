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

import static uk.ac.manchester.spinnaker.messages.model.RouterCommand.ROUTER_INIT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_RTR;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * An SCP request to clear the router on a chip. There is no response payload.
 * <p>
 * Calls {@code rtr_mc_init()} in {@code sark_hw.c}, via {@code rtr_cmd()} in
 * {@code scamp-cmd.c}.
 */
public class RouterClear extends SCPRequest<CheckOKResponse> {
	/**
	 * @param chip
	 *            The coordinates of the chip to clear the router of
	 */
	public RouterClear(HasChipLocation chip) {
		super(chip.getScampCore(), CMD_RTR, ROUTER_INIT.value);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Router Clear", CMD_RTR, buffer);
	}
}
