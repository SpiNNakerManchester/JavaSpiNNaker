/*
 * Copyright (c) 2019 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus.encodeTimeout;
import static uk.ac.manchester.spinnaker.messages.scp.ReinjectorCommand.SET_ROUTER_EMERGENCY_TIMEOUT;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request to set the router emergency timeout for dropped packet
 * reinjection. There is no response payload.
 * <p>
 * Handled by {@code reinjection_set_emergency_timeout_sdp()} in
 * {@code extra_monitor_support.c}.
 */
public final class SetRouterEmergencyTimeout
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
