/*
 * Copyright (c) 2021-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine.board;

import static java.lang.Integer.compare;
import static java.lang.Integer.parseInt;

import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 * Triad coordinates. Triads are addressed using (X,Y,Z) cooordinates, where Z
 * selects the member of a group of three (hence a range 0-2) and X and Y locate
 * that group within a rectangular grid across the machine.
 *
 * @author Donal Fellows
 */
public final class TriadCoords implements Comparable<TriadCoords> {
	/** The width and height of a triad, in chips. */
	private static final int TRIAD_CHIP_SIZE = 12;

	private static final int TRIAD_MAJOR_OFFSET = 8;

	private static final int TRIAD_MINOR_OFFSET = 4;

	/** Parses the string produced by {@link #toString()}. */
	private static final Pattern PATTERN =
			Pattern.compile("^\\[x:(\\d+),y:(\\d+),z:(\\d+)\\]$");

	/** X coordinate of triad. */
	@ValidTriadX
	public final int x;

	/** Y coordinate of triad. */
	@ValidTriadY
	public final int y;

	/** Z coordinate of triad. */
	@ValidTriadZ
	public final int z;

	/**
	 * Create an instance.
	 *
	 * @param x
	 *            X coordinate.
	 * @param y
	 *            Y coordinate.
	 * @param z
	 *            Z coordinate.
	 */
	@JsonCreator
	public TriadCoords(@JsonProperty("x") int x, @JsonProperty("y") int y,
			@JsonProperty("z") int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Create an instance from its serial form. The serial form (where the
	 * numbers may vary) is:
	 *
	 * <pre>
	 * [x:34,y:56,z:2]
	 * </pre>
	 *
	 * @param serialForm
	 *            The form to deserialise.
	 * @throws IllegalArgumentException
	 *             If the string is not in the right form.
	 */
	@JsonCreator
	public TriadCoords(String serialForm) {
		var m = PATTERN.matcher(serialForm);
		if (!m.matches()) {
			throw new IllegalArgumentException(
					"bad argument: " + serialForm);
		}
		int idx = 0;
		x = parseInt(m.group(++idx));
		y = parseInt(m.group(++idx));
		z = parseInt(m.group(++idx));
	}

	/**
	 * Convert these coordinates into a <em>machine-global</em> chip location.
	 *
	 * @return The chip location relative to the root of the machine.
	 */
	public ChipLocation asChipLocation() {
		int rootX = x * TRIAD_CHIP_SIZE;
		int rootY = y * TRIAD_CHIP_SIZE;
		switch (z) {
		case 1:
			rootX += TRIAD_MAJOR_OFFSET;
			rootY += TRIAD_MINOR_OFFSET;
			break;
		case 2:
			rootX += TRIAD_MINOR_OFFSET;
			rootY += TRIAD_MAJOR_OFFSET;
			break;
		case 0:
		default:
			break;
		}
		return new ChipLocation(rootX, rootY);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TriadCoords) {
			var other = (TriadCoords) obj;
			return x == other.x && y == other.y && z == other.z;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return x * 25 + y * 5 + z;
	}

	@Override
	public String toString() {
		return "[x:" + x + ",y:" + y + ",z:" + z + "]";
	}

	@Override
	public int compareTo(TriadCoords other) {
		int cmp = compare(x, other.x);
		if (cmp != 0) {
			return cmp;
		}
		cmp = compare(y, other.y);
		if (cmp != 0) {
			return cmp;
		}
		return compare(z, other.z);
	}
}
