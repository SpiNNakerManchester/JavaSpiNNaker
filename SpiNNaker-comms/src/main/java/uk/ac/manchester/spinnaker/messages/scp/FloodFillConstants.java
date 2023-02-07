/*
 * Copyright (c) 2023 The University of Manchester
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

/**
 * Various constants used in the flood fill sub-protocol.
 *
 * @author Donal Fellows
 */
abstract class FloodFillConstants {
	private FloodFillConstants() {
	}

	/** Send on all links. */
	static final int FORWARD_LINKS = 0x3F;

	/** Inter-send delay 24&mu;s. */
	static final int DELAY = 0x18;

	/** Number of times to resend a data message. */
	static final int DATA_RESEND = 2;

	/** Initial level. (What is level?) */
	static final int INIT_LEVEL = 3;

	/** Whether to issue an ID for the fill. */
	static final int ADD_ID = 1;
}
