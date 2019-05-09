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
package uk.ac.manchester.spinnaker.messages.sdp;

/** SDP port handling output buffering data streaming. */
public enum SDPPort {
	/** Default port. */
	DEFAULT_PORT(0),
	/** Command port for the buffered in functionality. */
	INPUT_BUFFERING_SDP_PORT(1),
	/** Command port for the buffered out functionality. */
	OUTPUT_BUFFERING_SDP_PORT(2),
	/** Command port for resetting runtime, etc. */
	RUNNING_COMMAND_SDP_PORT(3),
	/** Extra monitor core re injection functionality. */
	EXTRA_MONITOR_CORE_REINJECTION(4),
	/** Extra monitor core data transfer functionality. */
	EXTRA_MONITOR_CORE_DATA_SPEED_UP(5),
	/** Messages directed at the packet gatherer for the speed up protocols. */
	GATHERER_DATA_SPEED_UP(6);
	/** The port ID. */
	public final int value;

	SDPPort(int value) {
		this.value = value;
	}
}
