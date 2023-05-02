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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import static java.lang.Integer.compare;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.COORD_SHIFT;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.validateChipLocation;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

/**
 * The location of a {@link Chip} as an X and Y tuple.
 * <p>
 * This class is final as it is used a key in maps.
 *
 * @author Alan Stokes
 * @author Donal Fellows
 */
@Immutable
@JsonFormat(shape = ARRAY)
/*
 * This is needed in Java 8, but not by Java 10 (which can find the @JsonIgnore
 * annotation on the default property all by itself.
 */
@JsonIgnoreProperties({"scamp-core", "scamp_core", "scampCore"})
public final class ChipLocation
		implements HasChipLocation, Comparable<ChipLocation>, Serializable {
	private static final long serialVersionUID = -2343484354316378507L;

	/** The X coordinate. */
	@ValidX
	private final int x;

	/** The Y coordinate. */
	@ValidY
	private final int y;

	/**
	 * The location (0,0), which is in the bottom/left corner and typically the
	 * Ethernet-enabled chip for the lead board of an allocation. It is also
	 * typically the boot board of an allocation.
	 */
	public static final ChipLocation ZERO_ZERO = new ChipLocation(0, 0);

	/**
	 * The location (1,0), which is the one to the east/right of the bottom/left
	 * corner.
	 * <p>
	 * This location has special meaning on a 4 chip board.
	 */
	public static final ChipLocation ONE_ZERO = new ChipLocation(1, 0);

	/**
	 * Create the location of a chip on a SpiNNaker machine.
	 *
	 * @param x
	 *            The X coordinate
	 * @param y
	 *            The Y coordinate
	 * @throws IllegalArgumentException
	 *             Thrown is either x or y is negative or too big.
	 */
	@JsonCreator
	public ChipLocation(@JsonProperty(value = "x", required = true) int x,
			@JsonProperty(value = "y", required = true) int y) {
		validateChipLocation(x, y);
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof ChipLocation) {
			var that = (ChipLocation) obj;
			return (x == that.x) && (y == that.y);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (x << COORD_SHIFT) ^ y;
	}

	@Override
	public int compareTo(ChipLocation other) {
		int cmp = compare(x, other.x);
		if (cmp == 0) {
			cmp = compare(y, other.y);
		}
		return cmp;
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
	public String toString() {
		return "X:" + getX() + " Y:" + getY();
	}

	@Override
	public ChipLocation asChipLocation() {
		return this;
	}
}
