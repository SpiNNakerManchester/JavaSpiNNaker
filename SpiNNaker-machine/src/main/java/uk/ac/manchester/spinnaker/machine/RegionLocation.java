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
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.COORD_SHIFT;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.CORE_SHIFT;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.REGION_SHIFT;

import java.util.Comparator;

import com.google.errorprone.annotations.Immutable;

/**
 * Holding case for a CoreLocation (X, Y and P) and the recording region ID.
 *
 * @author Christian-B
 */
@Immutable
public final class RegionLocation
		implements HasCoreLocation, Comparable<RegionLocation> {
	/** The Chip / Core's X value. */
	@ValidX
	public final int x;

	/** The Chip / Core's Y value. */
	@ValidY
	public final int y;

	/** The Core's P value. */
	@ValidP
	public final int p;

	/** The recording region ID. */
	public final int region;

	/** Precalculated hashcode. */
	private final int hashcode;

	/**
	 * Creates the region based on a core and a region.
	 *
	 * @param core
	 *            The core to use
	 * @param region
	 *            The ID of the region to use.
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
