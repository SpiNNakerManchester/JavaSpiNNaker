/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.TOP_BIT;
import static uk.ac.manchester.spinnaker.messages.scp.RouterCommand.FIXED;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_RTR;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.RoutingEntry;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to read a fixed route entry. The response payload is the
 * {@linkplain RoutingEntry routing entry}.
 * <p>
 * Calls {@code rtr_fr_get()} in {@code sark_hw.c}, via {@code rtr_cmd()} in
 * {@code scamp-cmd.c}.
 */
public final class FixedRouteRead extends SCPRequest<FixedRouteRead.Response> {
	private static int argument1(AppID appID) {
		return (appID.appID() << BYTE1) | (FIXED.value << BYTE0);
	}

	// Top bit set = do a read
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
	public Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new Response(buffer);
	}

	/** Response for the fixed route read. */
	protected final class Response
			extends PayloadedResponse<RoutingEntry, RuntimeException> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read Fixed RoutingEntry route", CMD_RTR, buffer);
		}

		/** @return the fixed route router route. */
		@Override
		protected RoutingEntry parse(ByteBuffer buffer) {
			return new RoutingEntry(buffer.getInt());
		}
	}
}
