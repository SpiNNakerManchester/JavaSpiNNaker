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
	/** Set the router's main timeout. */
	SET_ROUTER_TIMEOUT(0),
	/** Set the router's emergency timeout. */
	SET_ROUTER_EMERGENCY_TIMEOUT(1),
	/** Set what packet types are reinjected. */
	SET_PACKET_TYPES(2),
	/** Get the status of the reinjector. */
	GET_STATUS(3),
	/** Reset the counters inside the reinjector. */
	RESET_COUNTERS(4),
	/** Stop the reinjector. */
	EXIT(5),
	/** Clear the reinjector's queues. */
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
