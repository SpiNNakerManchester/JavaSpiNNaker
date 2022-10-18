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
package uk.ac.manchester.spinnaker.alloc.web;

import java.util.List;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.model.ConnectionInfo;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;

/**
 * A sub-machine that just throws {@link UnsupportedOperationException} for
 * every operation. Subclass to add the behaviours you want.
 *
 * @author Donal Fellows
 */
public abstract class StubSubMachine implements SpallocAPI.SubMachine {
	@Override
	public Machine getMachine() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getRootX() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getRootY() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getRootZ() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getWidth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getDepth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<ConnectionInfo> getConnections() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<BoardCoordinates> getBoards() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PowerState getPower() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPower(PowerState powerState) {
		throw new UnsupportedOperationException();
	}
}
