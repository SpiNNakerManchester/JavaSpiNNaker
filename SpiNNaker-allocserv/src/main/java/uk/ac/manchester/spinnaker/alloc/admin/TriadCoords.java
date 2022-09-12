/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.admin;

import static java.lang.Integer.compare;
import static uk.ac.manchester.spinnaker.alloc.Constants.TRIAD_CHIP_SIZE;
import static uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.parseDec;

import java.util.regex.Pattern;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.PositiveOrZero;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.Machine;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.board.Direction;

/**
 * Triad coordinates.
 *
 * @author Donal Fellows
 */
public final class TriadCoords // FIXME
		implements Comparable<TriadCoords> {
	/** X coordinate. */
	@PositiveOrZero(message = "x coordinate must not be negative")
	public final int x;

	/** Y coordinate. */
	@PositiveOrZero(message = "y coordinate must not be negative")
	public final int y;

	/** Z coordinate. */
	@Min(value = 0, message = "z coordinate must not be negative")
	@Max(value = 2, message = "z coordinate must not be more than 2")
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

	private static final Pattern PATTERN =
			Pattern.compile("^\\[x:(\\d+),y:(\\d+),z:(\\d+)\\]$");

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
		x = parseDec(m.group(++idx));
		y = parseDec(m.group(++idx));
		z = parseDec(m.group(++idx));
	}

	private static final int TRIAD_MAJOR_OFFSET = 8;

	private static final int TRIAD_MINOR_OFFSET = 4;

	ChipLocation chipLocation() {
		int rootX = x * TRIAD_CHIP_SIZE;
		int rootY = y * TRIAD_CHIP_SIZE;
		switch (z) {
		case 0:
			break;
		case 1:
			rootX += TRIAD_MAJOR_OFFSET;
			rootY += TRIAD_MINOR_OFFSET;
			break;
		case 2:
			rootX += TRIAD_MINOR_OFFSET;
			rootY += TRIAD_MAJOR_OFFSET;
			break;
		default:
			throw new IllegalArgumentException("bad Z coordinate");
		}
		return new ChipLocation(rootX, rootY);
	}

	/**
	 * Applies a wraparound rule in a particular direction, turning coordinate
	 * space into something of a modular field.
	 *
	 * @param value
	 *            The value to wrap.
	 * @param limit
	 *            The upper limit. (Lower limits are always zero.)
	 * @return The potentially wrapped value.
	 */
	private static int limit(int value, int limit) {
		if (value < 0) {
			return value + limit;
		} else if (value >= limit) {
			return value - limit;
		} else {
			return value;
		}
	}

	/**
	 * Get the triad coordinate that you arrive at when you move from the
	 * current location in the indicated direction on the given machine. This
	 * ignores dead links and dead boards.
	 *
	 * @param direction
	 *            Which way to move
	 * @param machine
	 *            Used to determine where wraparounds are
	 * @return The new location
	 */
	TriadCoords move(Direction direction, Machine machine) {
		var di = DirInfo.get(z, direction);
		return new TriadCoords(limit(x + di.dx, machine.getWidth()),
				limit(y + di.dy, machine.getHeight()), z + di.dz);
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
		return (((x << 2 + x) ^ y) << 2 + y) ^ z;
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
