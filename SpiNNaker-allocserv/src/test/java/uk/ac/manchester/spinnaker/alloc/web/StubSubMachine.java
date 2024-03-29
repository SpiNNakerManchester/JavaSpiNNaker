/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
