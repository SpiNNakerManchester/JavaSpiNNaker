/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine;

import static uk.ac.manchester.spinnaker.machine.MachineDefaults.COORD_SHIFT;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.CORE_SHIFT;

/**
 * The location of a Core as an X, Y, P tuple.
 * <p>
 * This class is final as it is used a key in maps.
 *
 * @author alan
 * @author dkf
 */
public final class CoreLocation
		implements HasCoreLocation, Comparable<CoreLocation> {
	private final int x;

	private final int y;

	private final int p;

	/**
	 * Create the location of a core on a SpiNNaker machine.
	 *
	 * @param x
	 *            The X coordinate, in range 0..255
	 * @param y
	 *            The Y coordinate, in range 0..255
	 * @param p
	 *            The P coordinate, in range 0..17
	 */
	public CoreLocation(int x, int y, int p) {
		MachineDefaults.validateCoreLocation(x, y, p);
		this.x = x;
		this.y = y;
		this.p = p;
	}

	/**
	 * Create the location of a core on a SpiNNaker machine.
	 *
	 * @param chip
	 *            The X and Y coordinate, in range 0..255
	 * @param p
	 *            The P coordinate, in range 0..17
	 */
	public CoreLocation(HasChipLocation chip, int p) {
		this(chip.getX(), chip.getY(), p);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof CoreLocation)) {
			return false;
		}
		var that = (CoreLocation) obj;
		return (x == that.x) && (y == that.y) && (p == that.p);
	}

	@Override
	public int hashCode() {
		return (((x << COORD_SHIFT) ^ y) << CORE_SHIFT) ^ p;
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public int getP() {
		return p;
	}

	@Override
	public String toString() {
		return "X:" + getX() + " Y:" + getY() + " P:" + getP();
	}

	@Override
	public CoreLocation asCoreLocation() {
		return this;
	}

	@Override
	public int compareTo(CoreLocation other) {
		if (x < other.x) {
			return -1;
		} else if (x > other.x) {
			return 1;
		}
		if (y < other.y) {
			return -1;
		} else if (y > other.y) {
			return 1;
		}
		if (p < other.p) {
			return -1;
		} else if (p > other.p) {
			return 1;
		}
		return 0;
	}
}
