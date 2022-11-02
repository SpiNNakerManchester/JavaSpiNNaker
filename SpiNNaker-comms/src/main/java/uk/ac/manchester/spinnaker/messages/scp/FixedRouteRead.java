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
import static uk.ac.manchester.spinnaker.messages.scp.Bits.TOP_BIT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_RTR;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.RoutingEntry;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** Gets a fixed route entry. */
public final class FixedRouteRead extends SCPRequest<FixedRouteRead.Response> {
	private static final int MAGIC = 3;

	private static int argument1(AppID appID) {
		return (appID.appID << BYTE1) | (MAGIC << BYTE0);
	}

	private static int argument2() {
		return 1 << TOP_BIT;
	}

	/**
	 * @param chip
	 *            The chip to get the route from.
	 * @param appID
	 *            The ID of the application associated with the route
	 */
	public FixedRouteRead(HasChipLocation chip, AppID appID) {
		super(chip.getScampCore(), CMD_RTR, argument1(appID), argument2());
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** Response for the fixed route read. */
	public static final class Response extends CheckOKResponse {
		private final int route;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read Fixed Route", CMD_RTR, buffer);
			route = buffer.getInt();
		}

		/** @return the fixed route router route. */
		public RoutingEntry getRoute() {
			return new RoutingEntry(route);
		}
	}
}
