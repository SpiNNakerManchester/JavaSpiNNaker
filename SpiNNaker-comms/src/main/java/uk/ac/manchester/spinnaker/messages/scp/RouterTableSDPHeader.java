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

import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.GATHERER_DATA_SPEED_UP;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/**
 * Variant of SCP that talks to the packet reinjector for doing saving and
 * loading of multicast router tables.
 *
 * @author Donal Fellows
 */
class RouterTableSDPHeader extends SDPHeader {
	/**
	 * Make a header.
	 *
	 * @param core
	 *            The SpiNNaker core that we want to talk to. Should be running
	 *            the extra monitor core.
	 */
	RouterTableSDPHeader(HasCoreLocation core) {
		super(REPLY_EXPECTED, core, GATHERER_DATA_SPEED_UP);
	}
}
