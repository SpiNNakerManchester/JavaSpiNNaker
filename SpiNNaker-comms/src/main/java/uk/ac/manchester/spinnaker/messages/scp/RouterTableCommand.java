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

/** SDP Command codes to the extra monitors for router table control. */
enum RouterTableCommand implements CommandCode {
	/**
	 * Save the application multicast routes, which must be currently installed.
	 */
	SAVE_APPLICATION_ROUTES(6),
	/** Load the (previously saved) application multicast routes. */
	LOAD_APPLICATION_ROUTES(7),
	/** Load the (previously configured) system multicast routes. */
	LOAD_SYSTEM_ROUTES(8);

	/**
	 * The encoded form of the command.
	 */
	private final short value;

	RouterTableCommand(int cmd) {
		this.value = (short) cmd;
	}

	@Override
	public short getValue() {
		return value;
	}
}
