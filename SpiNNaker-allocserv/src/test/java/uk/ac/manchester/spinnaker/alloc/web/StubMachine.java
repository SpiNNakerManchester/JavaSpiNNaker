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

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.model.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.model.DownLink;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;

/**
 * A machine that just throws {@link UnsupportedOperationException} for every
 * operation. Subclass to add the behaviours you want.
 *
 * @author Donal Fellows
 */
public abstract class StubMachine implements Machine {
	@Override
	public void waitForChange(Duration timeout) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> getTags() {
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
	public boolean isInService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<BoardCoords> getDeadBoards() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<DownLink> getDownLinks() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<BoardLocation> getBoardByChip(HasChipLocation chip) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<BoardLocation> getBoardByPhysicalCoords(
			PhysicalCoords coords) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<BoardLocation> getBoardByLogicalCoords(TriadCoords coords) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<BoardLocation> getBoardByIPAddress(String address) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getRootBoardBMPAddress() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Integer> getBoardNumbers() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Integer> getAvailableBoards() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getBMPAddress(BMPCoords bmp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Integer> getBoardNumbers(BMPCoords bmp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean equals(Object other) {
		if (other instanceof Machine) {
			var m = (Machine) other;
			return getId() == m.getId();
		}
		return false;
	}

	@Override
	public final int hashCode() {
		return getId();
	}
}
