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

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.DownLink;

/**
 * A machine that just throws {@link UnsupportedOperationException} for every
 * operation. Subclass to add the behaviours you want.
 *
 * @author Donal Fellows
 */
public class StubMachine implements SpallocAPI.Machine {
	@Override
	public void waitForChange(long timeout) {
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
	public List<String> getTags() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getWidth() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getHeight() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<BoardCoords> getDeadBoards() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<DownLink> getDownLinks() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<BoardLocation> getBoardByChip(int x, int y)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<BoardLocation> getBoardByPhysicalCoords(int cabinet,
			int frame, int board) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<BoardLocation> getBoardByLogicalCoords(int x, int y, int z)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<BoardLocation> getBoardByIPAddress(String address)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getRootBoardBMPAddress() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Integer> getBoardNumbers() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Integer> getAvailableBoards() throws SQLException {
		throw new UnsupportedOperationException();
	}
}
