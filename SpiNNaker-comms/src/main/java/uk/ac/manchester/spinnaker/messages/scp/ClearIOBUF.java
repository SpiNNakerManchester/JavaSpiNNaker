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

import static uk.ac.manchester.spinnaker.messages.scp.RunningCommand.CLEAR_IOBUF;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * An SCP Request to clear the IOBUF on a core.
 */
public class ClearIOBUF extends SimpleRequest {
	/**
	 * @param core
	 *            The core to clear the IOBUF of.
	 */
	public ClearIOBUF(HasCoreLocation core) {
		super("clear iobuf", core, CLEAR_IOBUF, 0, 0, 1, null);
	}
}
