/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.allocator;

import java.util.List;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.Connection;

public class SubMachine {
	/** The machine that this sub-machine is part of. */
	public Machine machine;

	/** The width of this sub-machine. */
	public int width;

	/** The height of this sub-machine. */
	public int height;

	/** The connection details of this sub-machine. */
	public List<Connection> connections; // FIXME

	/** The board locations of this sub-machine. */
	public List<BoardCoordinates> boards; // FIXME

	public PowerState getPower() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setPower(PowerState ps) {
		// TODO Auto-generated method stub
	}
}
