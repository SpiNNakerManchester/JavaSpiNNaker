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
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.validateCoreLocation;

import java.io.Serializable;

import com.google.errorprone.annotations.Immutable;

/**
 * The location of a Core as an X, Y, P tuple.
 *
 * @author alan
 * @author dkf
 * @param x
 *            The X coordinate, in range 0..255
 * @param y
 *            The Y coordinate, in range 0..255
 * @param p
 *            The P coordinate, in range 0..17
 */
@Immutable
public record CoreLocation(@ValidX int x, @ValidY int y, @ValidP int p)
		implements HasCoreLocation, Comparable<CoreLocation>, Serializable {
	public CoreLocation {
		validateCoreLocation(x, y, p);
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
