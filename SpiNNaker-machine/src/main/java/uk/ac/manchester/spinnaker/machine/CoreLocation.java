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

import static java.lang.Integer.compare;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.COORD_SHIFT;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.CORE_SHIFT;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.validateCoreLocation;

import java.io.Serializable;

import com.google.errorprone.annotations.Immutable;

/**
 * The location of a {@link Processor} on a {@link Chip} as an X, Y, P tuple.
 * <p>
 * This class is final as it is used a key in maps.
 *
 * @author Alan Stokes
 * @author Donal Fellows
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
		if (obj instanceof CoreLocation) {
			var that = (CoreLocation) obj;
			return (x == that.x) && (y == that.y) && (p == that.p);
		}
		return false;
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
