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
package uk.ac.manchester.spinnaker.messages.boot;

import static uk.ac.manchester.spinnaker.messages.boot.BootOpCode.FLOOD_FILL_START;

/**
 * The message indicating the start of a flood fill for booting.
 *
 * @author Donal Fellows
 */
class StartOfBootMessages extends BootMessage {
	/**
	 * @param numPackets
	 *            The number of payload packets to be sent.
	 */
	StartOfBootMessages(int numPackets) {
		super(FLOOD_FILL_START, 0, 0, numPackets - 1);
	}
}
