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
package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.STOP_SENDING_REQUESTS;

/**
 * Packet used in the context of buffering input for the host computer to signal
 * to the SpiNNaker system that to stop sending "SpinnakerRequestBuffers"
 * packet.
 */
public class StopRequests extends EIEIOCommandMessage {
	public StopRequests() {
		super(STOP_SENDING_REQUESTS);
	}
}
