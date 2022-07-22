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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import static java.lang.Integer.compare;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.COORD_SHIFT;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.validateChipLocation;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The location of a Chip as an X and Y tuple.
 * <p>
 * This class is final as it is used a key in maps.
 *
 * @author alan
 * @author dkf
 */
@JsonFormat(shape = ARRAY)
/*
 * This is needed in Java 8, but not by Java 10 (which can find the @JsonIgnore
 * annotation on the default property all by itself.
 */
@JsonIgnoreProperties({"scamp-core", "scamp_core", "scampCore"})
public final class ChipLocation
		implements HasChipLocation, Comparable<ChipLocation>, Serializable {
	private static final long serialVersionUID = -2343484354316378507L;

	private final int x;

	private final int y;

	/**
	 * The location (0,0), which is in the bottom/left corner and typically the
	 * ethernet chip for the lead board of an allocation.
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
		if (!(obj instanceof ChipLocation)) {
			return false;
		}
		var that = (ChipLocation) obj;
		return (x == that.x) && (y == that.y);
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
