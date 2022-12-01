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

import java.util.Comparator;

import com.google.errorprone.annotations.Immutable;

/**
 * Holding case for a CoreLocation (X, Y and P) and the recording region ID.
 *
 * @author Christian
 * @param x
 *            The Chip / Core's X value.
 * @param y
 *            The Chip / Core's Y value.
 * @param p
 *            The Core's P value.
 * @param region
 *            The recording Region.
 */
@Immutable
public record RegionLocation(//
		@ValidX int x, @ValidY int y, @ValidP int p, int region)
		implements HasCoreLocation, Comparable<RegionLocation> {
	/**
	 * Creates the Region based on a Core and a region.
	 *
	 * @param core
	 *            The Core to use
	 * @param region
	 *            The Region to use.
	 */
	public RegionLocation(HasCoreLocation core, int region) {
		this(core.getX(), core.getY(), core.getP(), region);
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
	public String toString() {
		return "X:" + x + " Y:" + y + " P:" + p + "R: " + region;
	}
}
