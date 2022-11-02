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

import static uk.ac.manchester.spinnaker.messages.scp.ReinjectorCommand.CLEAR;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/** An SCP Request to clear the dropped packet reinjection queue. */
public class ClearReinjectionQueue extends SimpleRequest {
	/**
	 * @param core
	 *            The coordinates of the monitor core.
	 */
	public ClearReinjectionQueue(HasCoreLocation core) {
		super("Clear dropped packet reinjection queue", core, CLEAR, 0, 0, 0,
				null);
	}
}
