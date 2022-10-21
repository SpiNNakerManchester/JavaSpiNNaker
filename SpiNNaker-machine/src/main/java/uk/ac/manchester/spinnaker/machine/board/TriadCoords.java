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

import java.io.IOException;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 * Triad coordinates.
 * Boards are in groups of three (triads) that group together in a rectangular
 * grid. The {@code x} and {@code y} coordinates say which group of three in the
 * grid, and the {@code z} coordinate says which board within the group. The
 * group is not itself rectangular, but tesselates on a rectangular grid.
 * <p>
 * To understand how the triad coordinate system works, consider this board
 * layout (a classic 24 board machine, with wrap-arounds not shown):
 * <p>
 * <img src="doc-files/DirInfo1.png" width="450" alt="24-board layout">
 * <h2>Serialisation Formats</h2>
 * Defaults to being serialised as a JSON object:
 * <pre>{"x": 3, "y": 2, "z": 1}</pre>
 * and can also be deserialized from that, but can also accept being
 * deserialised from a JSON array:
 * <pre>[3, 2, 1]</pre>
 * and can also be deserialized from its {@linkplain #toString() string form}:
 * <pre>[x:3,y:2,z:1]</pre>
 *
 * @author Donal Fellows
 */
@JsonDeserialize(using = TriadCoords.Deserializer.class)
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
	 * [x:3,y:5,z:2]
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

	/** JSON deserializer for {@link TriadCoords}. */
	static final class Deserializer extends DeserializerHelper<TriadCoords> {
		private static final long serialVersionUID = 1L;

		protected Deserializer() {
			super(TriadCoords.class);
		}

		@Override
		TriadCoords deserializeArray() throws IOException {
			int x = getNextIntOfArray();
			int y = getNextIntOfArray();
			int z = getNextIntOfArray();
			requireEndOfArray();
			return new TriadCoords(x, y, z);
		}

		@Override
		TriadCoords deserializeObject() throws IOException {
			Integer x = null, y = null, z = null;
			String name;
			while ((name = getNextFieldName()) != null) {
				switch (name) {
				case "x":
					x = requireSetOnceInt(name, x);
					break;
				case "y":
					y = requireSetOnceInt(name, y);
					break;
				case "z":
					z = requireSetOnceInt(name, z);
					break;
				default:
					unknownProperty(name);
				}
			}
			if (x == null || y == null || z == null) {
				missingProperty("x", x, "y", y, "z", z);
			}
			return new TriadCoords(x, y, z);
		}

		@Override
		TriadCoords deserializeString(String string) {
			return new TriadCoords(string);
		}
	}
}
