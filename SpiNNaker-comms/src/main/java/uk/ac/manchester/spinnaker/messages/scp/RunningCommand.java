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

/** SCP Command codes for running core control. */
enum RunningCommand implements CommandCode {
	/** Ask the core to stop. */
	STOP_ID(6),
	/** Set the running time and mode. */
	NEW_RUNTIME_ID(7),
	/** Update the provenance and exit. */
	UPDATE_PROVENCE_REGION_AND_EXIT(8),
	/** Clear the IOBUF of the core. */
	CLEAR_IOBUF(9);

	/**
	 * The encoded form of the command.
	 */
	private final short value;

	RunningCommand(int cmd) {
		this.value = (short) cmd;
	}

	@Override
	public short getValue() {
		return value;
	}
}
