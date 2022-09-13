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
package uk.ac.manchester.spinnaker.machine.board;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import static java.lang.Integer.compare;
import static java.lang.Integer.parseUnsignedInt;
import static uk.ac.manchester.spinnaker.machine.board.Direction.E;
import static uk.ac.manchester.spinnaker.machine.board.Direction.N;
import static uk.ac.manchester.spinnaker.machine.board.Direction.NW;
import static uk.ac.manchester.spinnaker.machine.board.Direction.S;
import static uk.ac.manchester.spinnaker.machine.board.Direction.SE;
import static uk.ac.manchester.spinnaker.machine.board.Direction.W;
import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.regex.Pattern;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.PositiveOrZero;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 * Triad coordinates.
 * <p>
 * Consider this board layout (a classic 24 board machine, with wrap-arounds not
 * shown):
 * <p>
 * <img src="doc-files/DirInfo1.png" width="450" alt="24-board layout">
 *
 * @author Donal Fellows
 */
@JsonPropertyOrder({
	"x", "y", "z"
})
@JsonFormat(shape = ARRAY)
public final class TriadCoords implements Comparable<TriadCoords> {
	/** The number of boards in a triad. */
	public static final int TRIAD_DEPTH = 3;

	/** The width and height of a triad, in chips. */
	public static final int TRIAD_CHIP_SIZE = 12;

	/** X coordinate. */
	@PositiveOrZero(message = "x coordinate must not be negative")
	private final int x;

	/** Y coordinate. */
	@PositiveOrZero(message = "y coordinate must not be negative")
	private final int y;

	/** Z coordinate. */
	@Min(value = 0, message = "z coordinate must not be negative")
	@Max(value = 2, message = "z coordinate must not be more than 2")
	private final int z;

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
		var m = PATTERN.matcher(requireNonNull(serialForm));
		if (!m.matches()) {
			throw new IllegalArgumentException(
					"bad argument: " + serialForm);
		}
		int idx = 0;
		x = parseUnsignedInt(m.group(++idx));
		y = parseUnsignedInt(m.group(++idx));
		z = parseUnsignedInt(m.group(++idx));
	}

	/** @return The X coordinate of the board. */
	public int getX() {
		return x;
	}

	/** @return The Y coordinate of the board. */
	public int getY() {
		return y;
	}

	/** @return The Z coordinate of the board. */
	public int getZ() {
		return z;
	}

	private static final int TRIAD_MAJOR_OFFSET = 8;

	private static final int TRIAD_MINOR_OFFSET = 4;

	/**
	 * Get the computed location of the root ethernet chip of a board.
	 *
	 * @return The location of the chip, in machine-global chip coordinates.
	 * @throws IllegalArgumentException
	 *             If there's a bad configuration. Should be unreachable.
	 */
	public ChipLocation chipLocation() {
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
	 * How much to change a triad coordinate by to move in a particular
	 * direction from a board.
	 */
	// TODO make into a record once Java language profile is new enough
	static final class Delta {
		/** Change your X coordinate by this. */
		final int dx;

		/** Change your Y coordinate by this. */
		final int dy;

		/** Change your Z coordinate by this. */
		final int dz;

		private Delta(int dx, int dy, int dz) {
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
		}
	}

	/**
	 * A mapping that says how to go from one board's coordinates (only the Z
	 * coordinate matters for this) to another when you move in a particular
	 * direction. Assumes that we are handling a SpiNN-5 board.
	 * They're also in spalloc server's DB's {@code movement_directions} table.
	 * <p>
	 * Consider this board layout (a classic 24 board machine, with wrap-arounds
	 * not shown):
	 * <p>
	 * <img src="doc-files/DirInfo1.png" width="450" alt="24-board layout">
	 * <p>
	 * Bear in mind that 0,1,0 is <em>actually</em> 12 chips vertically and 0
	 * chips horizontally offset from 0,0,0. (Also, the real boards are slightly
	 * offset from this layout.)
	 *
	 * @author Donal Fellows
	 * @see Direction
	 * @see TriadCoords
	 */
	private static final Map<Integer, Map<Direction, Delta>> MOVES = Map.of(
			// When Z = 0
			0, Map.ofEntries(//
					entry(N,  new Delta(+0, +0, +2)),
					entry(E,  new Delta(+0, +0, +1)),
					entry(SE, new Delta(+0, -1, +2)),
					entry(S,  new Delta(-1, -1, +1)),
					entry(W,  new Delta(-1, -1, +2)),
					entry(NW, new Delta(-1, +0, +1))),
			// When Z = 1
			1, Map.ofEntries(//
					entry(N,  new Delta(+1, +1, -1)),
					entry(E,  new Delta(+1, +0, +1)),
					entry(SE, new Delta(+1, +0, -1)),
					entry(S,  new Delta(+0, -1, +1)),
					entry(W,  new Delta(+0, +0, -1)),
					entry(NW, new Delta(+0, +0, +1))),
			// When Z = 2
			2, Map.ofEntries(//
					entry(N,  new Delta(+0, +1, -1)),
					entry(E,  new Delta(+1, +1, -2)),
					entry(SE, new Delta(+0, +0, -1)),
					entry(S,  new Delta(+0, +0, -2)),
					entry(W,  new Delta(-1, +0, -1)),
					entry(NW, new Delta(+0, +1, -2))));

	/**
	 * Get the triad coordinate that you arrive at when you move from the
	 * current location in the indicated direction on the given machine. This
	 * ignores dead links and dead boards; it is a geometric determination only.
	 *
	 * @param direction
	 *            Which way to move
	 * @param machineDimensions
	 *            Used to determine where wraparounds are
	 * @return The new location
	 */
	public TriadCoords move(Direction direction,
			MachineBoardDimensions machineDimensions) {
		var di = MOVES.get(z).get(direction);
		return new TriadCoords(limit(x + di.dx, machineDimensions.width),
				limit(y + di.dy, machineDimensions.height), z + di.dz);
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
		requireNonNull(other);
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
