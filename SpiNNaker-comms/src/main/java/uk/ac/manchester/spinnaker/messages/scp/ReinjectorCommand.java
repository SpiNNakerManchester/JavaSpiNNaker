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

/** SCP Command codes for reinjection. */
enum ReinjectorCommand implements CommandCode {
	// TODO doc
	SET_ROUTER_TIMEOUT(0),
	// TODO doc
	SET_ROUTER_EMERGENCY_TIMEOUT(1),
	// TODO doc
	SET_PACKET_TYPES(2),
	// TODO doc
	GET_STATUS(3),
	// TODO doc
	RESET_COUNTERS(4),
	// TODO doc
	EXIT(5),
	// TODO doc
	CLEAR(6);

	/**
	 * The encoded form of the command.
	 */
	private final short value;

	ReinjectorCommand(int cmd) {
		this.value = (short) cmd;
	}

	@Override
	public short getValue() {
		return value;
	}
}
