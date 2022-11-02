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

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * An SCP Request to set the router emergency timeout for dropped packet
 * reinjection.
 */
public class SetRouterEmergencyTimeout extends SimpleRequest {
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
		super("Set router emergency timeout", core,
				SET_ROUTER_EMERGENCY_TIMEOUT,
				encodeTimeout(timeoutMantissa, timeoutExponent), 0, 0, null);
	}
}
