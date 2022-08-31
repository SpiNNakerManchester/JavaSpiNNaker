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

import static java.util.Comparator.comparing;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.COORD_SHIFT;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.CORE_SHIFT;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.REGION_SHIFT;

import java.util.Comparator;

/**
 * Holding case for a CoreLocation (X, Y and P) and the recording region ID.
 *
 * @author Christian
 */
public class RegionLocation
		implements HasCoreLocation, Comparable<RegionLocation> {
	/** The Chip / Core's X value. */
	public final int x;

	/** The Chip / Core's Y value. */
	public final int y;

	/** The Core's P value. */
	public final int p;

	/** The recording Region. */
	public final int region;

	/** Precalculated hashcode. */
	public final int hashcode;

	/**
	 * Creates the Region based on a Core and a region.
	 *
	 * @param core
	 *            The Core to use
	 * @param region
	 *            The Region to use.
	 */
	public RegionLocation(HasCoreLocation core, int region) {
		x = core.getX();
		y = core.getY();
		p = core.getP();
		this.region = region;
		hashcode = ((((x << COORD_SHIFT) ^ y) << CORE_SHIFT)
				^ p) << REGION_SHIFT ^ region;
	}

	@Override
	public int getP() {
		return p;
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	/** Comparator for region locations. */
	public static final Comparator<RegionLocation> COMPARATOR =
			comparing(RegionLocation::getX) //
					.thenComparing(RegionLocation::getY)
					.thenComparing(RegionLocation::getP)
					.thenComparing(rl -> rl.region);

	@Override
	public int compareTo(RegionLocation o) {
		return COMPARATOR.compare(this, o);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof RegionLocation)) {
			return false;
		}
		var that = (RegionLocation) obj;
		return (x == that.x) && (y == that.y) && (p == that.p)
				&& (region == that.region);
	}

	@Override
	public String toString() {
		return "X:" + x + " Y:" + y + " P:" + p + "R: " + region;
	}

	@Override
	public int hashCode() {
		return hashcode;
	}
}
