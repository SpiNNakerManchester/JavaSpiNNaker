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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Physical board coordinates.
 *
 * @author Donal Fellows
 */
public final class PhysicalCoords implements Comparable<PhysicalCoords> {
	/** Cabinet number. */
	@ValidCabinetNumber
	public final int c;

	/** Frame number. */
	@ValidFrameNumber
	public final int f;

	/** Board number. */
	@ValidBoardNumber
	public final int b;

	/**
	 * Create an instance.
	 *
	 * @param c
	 *            Cabinet number.
	 * @param f
	 *            Frame number.
	 * @param b
	 *            Board number.
	 */
	@JsonCreator
	public PhysicalCoords(@JsonProperty("c") int c, @JsonProperty("f") int f,
			@JsonProperty("b") int b) {
		this.c = c;
		this.f = f;
		this.b = b;
	}

	private static final Pattern PATTERN =
			Pattern.compile("^\\[c:(\\d+),f:(\\d+),b:(\\d+)\\]$");

	/**
	 * Create an instance from its serial form. The serial form (where the
	 * numbers may vary) is:
	 *
	 * <pre>
	 * [c:34,f:12,b:23]
	 * </pre>
	 *
	 * @param serialForm
	 *            The form to deserialise.
	 * @throws IllegalArgumentException
	 *             If the string is not in the right form.
	 */
	@JsonCreator
	public PhysicalCoords(String serialForm) {
		var m = PATTERN.matcher(serialForm);
		if (!m.matches()) {
			throw new IllegalArgumentException("bad argument: " + serialForm);
		}
		int idx = 0;
		c = parseInt(m.group(++idx));
		f = parseInt(m.group(++idx));
		b = parseInt(m.group(++idx));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PhysicalCoords) {
			var other = (PhysicalCoords) obj;
			return c == other.c && f == other.f && b == other.b;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return c * 25 + f * 5 + b;
	}

	@Override
	public String toString() {
		return "[c:" + c + ",f:" + f + ",b:" + b + "]";
	}

	/**
	 * @return The coordinates of the BMP that manages this board.
	 */
	@JsonIgnore
	public BMPCoords getBmpCoords() {
		return new BMPCoords(c, f);
	}

	@Override
	public int compareTo(PhysicalCoords other) {
		int cmp = compare(c, other.c);
		if (cmp != 0) {
			return cmp;
		}
		cmp = compare(f, other.f);
		if (cmp != 0) {
			return cmp;
		}
		return compare(b, other.b);
	}
}
