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
package uk.ac.manchester.spinnaker.machine.board;

import static java.util.Objects.requireNonNull;

/**
 * A mapping that says how to go from one board's coordinates (only the Z
 * coordinate matters for this) to another when you move in a particular
 * direction. Assumes that we are handling a SpiNN-5 board.
 * <p>
 * Consider this board layout (a classic 24 board machine, with wrap-arounds not
 * shown):
 * <p>
 * <img src="doc-files/DirInfo1.png" width="450" alt="24-board layout">
 * <p>
 * Bear in mind that 0,1,0 is <em>actually</em> 12 chips vertically and 0 chips
 * horizontally offset from 0,0,0. (Also, the real boards are slightly offset
 * from this layout.)
 *
 * @author Donal Fellows
 * @see Direction
 * @see TriadCoords
 */
public final class DirInfo {
	/** When your Z coordinate is this. */
	public final int z;

	/** When you are moving in this direction. */
	public final Direction dir;

	/** Change your X coordinate by this. */
	public final int dx;

	/** Change your Y coordinate by this. */
	public final int dy;

	/** Change your Z coordinate by this. */
	public final int dz;

	DirInfo(int z, Direction d, int dx, int dy, int dz) {
		this.z = z;
		this.dir = requireNonNull(d);
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DirInfo)) {
			return false;
		}
		var di = (DirInfo) o;
		return z == di.z && dir == di.dir;
	}

	@Override
	public int hashCode() {
		return z ^ dir.hashCode();
	}
}
