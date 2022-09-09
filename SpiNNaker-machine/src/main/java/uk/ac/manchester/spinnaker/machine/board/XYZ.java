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

/** Triad coordinates. May be a hash table key. */
public final class XYZ { // FIXME
	/** X coordinate. */
	public final int x;

	/** Y coordinate. */
	public final int y;

	/** Z coordinate. */
	public final int z;

	/**
	 * @param x X coordinate.
	 * @param y Y coordinate.
	 * @param z Z coordinate.
	 */
	public XYZ(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof XYZ) {
			var other = (XYZ) obj;
			return x == other.x && y == other.y && z == other.z;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (((x << 2 + x) ^ y) << 2 + y) ^ z;
	}

	@Override
	public String toString() {
		return "[x:" + x + ",y:" + y + ",z:" + z + "]";
	}
}
