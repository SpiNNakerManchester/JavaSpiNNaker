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

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
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
public abstract class StubMachine implements SpallocAPI.Machine {
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
		if (other instanceof SpallocAPI.Machine) {
			SpallocAPI.Machine m = (Machine) other;
			return getId() == m.getId();
		}
		return false;
	}

	@Override
	public final int hashCode() {
		return getId();
	}
}
