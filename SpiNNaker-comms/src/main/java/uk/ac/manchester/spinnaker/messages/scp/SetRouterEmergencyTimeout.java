/*
 * Copyright (c) 2019 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus.encodeTimeout;
import static uk.ac.manchester.spinnaker.messages.scp.ReinjectorCommand.SET_ROUTER_EMERGENCY_TIMEOUT;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP Request to set the router emergency timeout for dropped packet
 * reinjection. There is no response payload.
 * <p>
 * Handled by {@code reinjection_set_emergency_timeout_sdp()} in
 * {@code extra_monitor_support.c}.
 */
public class SetRouterEmergencyTimeout
		extends ReinjectorRequest<EmptyResponse> {
	/**
	 * @param core
	 *            The coordinates of the monitor core.
	 * @param timeoutMantissa
	 *            The mantissa of the timeout value, between 0 and 15.
	 * @param timeoutExponent
	 *            The exponent of the timeout value, between 0 and 15.
	 */
	public SetRouterEmergencyTimeout(HasCoreLocation core, int timeoutMantissa,
			int timeoutExponent) {
		super(core, SET_ROUTER_EMERGENCY_TIMEOUT,
				encodeTimeout(timeoutMantissa, timeoutExponent));
	}

	@Override
	public EmptyResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new EmptyResponse("Set router emergency timeout",
				SET_ROUTER_EMERGENCY_TIMEOUT, buffer);
	}
}
