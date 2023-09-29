/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.machine;

import static java.util.Comparator.comparing;

import java.util.Comparator;

import com.google.errorprone.annotations.Immutable;

/**
 * Container for a CoreLocation (X, Y and P) and the recording region ID.
 *
 * @author Christian-B
 * @param x
 *            The Chip / Core's X value.
 * @param y
 *            The Chip / Core's Y value.
 * @param p
 *            The Core's P value.
 * @param region
 *            The recording region ID.
 */
@Immutable
public record RegionLocation(//
		@ValidX int x, @ValidY int y, @ValidP int p, int region)
		implements HasCoreLocation, Comparable<RegionLocation> {
	/**
	 * Creates the region based on a core and a region.
	 *
	 * @param core
	 *            The core to use
	 * @param region
	 *            The ID of the region to use.
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
			comparing(RegionLocation::x) //
					.thenComparing(RegionLocation::y)
					.thenComparing(RegionLocation::p)
					.thenComparing(RegionLocation::region);

	@Override
	public int compareTo(RegionLocation o) {
		return COMPARATOR.compare(this, o);
	}

	@Override
	public String toString() {
		return "X:" + x + " Y:" + y + " P:" + p + "R: " + region;
	}
}
