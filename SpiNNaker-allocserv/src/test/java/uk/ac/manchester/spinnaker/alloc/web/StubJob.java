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
import java.time.Instant;
import java.util.Optional;

import uk.ac.manchester.spinnaker.alloc.allocator.JobState;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.SubMachine;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 * A job that just throws {@link UnsupportedOperationException} for every
 * operation. Subclass to add the behaviours you want.
 *
 * @author Donal Fellows
 */
public class StubJob implements SpallocAPI.Job {
	@Override
	public void waitForChange(long timeout) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void access(String keepaliveAddress) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void destroy(String reason) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JobState getState() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Instant getStartTime() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> getKeepaliveHost() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Instant getKeepaliveTimestamp() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> getOwner() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<byte[]> getOriginalRequest() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Instant> getFinishTime() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> getReason() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<SubMachine> getMachine() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<BoardLocation> whereIs(int x, int y) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<ChipLocation> getRootChip() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Integer> getWidth() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Integer> getHeight() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Integer> getDepth() throws SQLException {
		throw new UnsupportedOperationException();
	}
}
