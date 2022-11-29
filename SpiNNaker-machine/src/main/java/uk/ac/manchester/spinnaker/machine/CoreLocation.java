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

import static java.lang.Integer.compare;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.COORD_SHIFT;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.CORE_SHIFT;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.validateCoreLocation;

import java.io.Serializable;

import com.google.errorprone.annotations.Immutable;

/**
 * The location of a Core as an X, Y, P tuple.
 * <p>
 * This class is final as it is used a key in maps.
 *
 * @author alan
 * @author dkf
 */
@Immutable
public final class CoreLocation
		implements HasCoreLocation, Comparable<CoreLocation>, Serializable {
	private static final long serialVersionUID = 2930811082362121057L;

	/** The X coordinate. */
	@ValidX
	private final int x;

	/** The Y coordinate. */
	@ValidY
	private final int y;

	/** The P coordinate. */
	@ValidP
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
		validateCoreLocation(x, y, p);
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
		return (obj instanceof CoreLocation that) && (x == that.x)
				&& (y == that.y) && (p == that.p);
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
		int cmp = compare(this.x, other.x);
		if (cmp == 0) {
			cmp = compare(this.y, other.y);
			if (cmp == 0) {
				cmp = compare(this.p, other.p);
			}
		}
		return cmp;
	}
}
