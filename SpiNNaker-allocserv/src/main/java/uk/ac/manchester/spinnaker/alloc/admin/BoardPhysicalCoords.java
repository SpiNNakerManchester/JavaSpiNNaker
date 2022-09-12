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
import static uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.parseDec;

import java.util.regex.Pattern;

import javax.validation.constraints.PositiveOrZero;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.manchester.spinnaker.machine.board.BMPCoords;

/**
 * Physical board coordinates.
 *
 * @author Donal Fellows
 */
public final class BoardPhysicalCoords // FIXME
		implements Comparable<BoardPhysicalCoords> {
	/** Cabinet number. */
	@PositiveOrZero(message = "cabinet number must not be negative")
	public final int c;

	/** Frame number. */
	@PositiveOrZero(message = "frame number must not be negative")
	public final int f;

	/** Board number. */
	@PositiveOrZero(message = "board number must not be negative")
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
	public BoardPhysicalCoords(@JsonProperty("c") int c,
			@JsonProperty("f") int f, @JsonProperty("b") int b) {
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
	public BoardPhysicalCoords(String serialForm) {
		var m = PATTERN.matcher(serialForm);
		if (!m.matches()) {
			throw new IllegalArgumentException(
					"bad argument: " + serialForm);
		}
		int idx = 0;
		c = parseDec(m.group(++idx));
		f = parseDec(m.group(++idx));
		b = parseDec(m.group(++idx));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BoardPhysicalCoords) {
			var other = (BoardPhysicalCoords) obj;
			return c == other.c && f == other.f && b == other.b;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (((c << 2 + c) ^ f) << 2 + f) ^ b;
	}

	@Override
	public String toString() {
		return "[c:" + c + ",f:" + f + ",b:" + b + "]";
	}

	BMPCoords bmp() {
		return new BMPCoords(c, f);
	}

	@Override
	public int compareTo(BoardPhysicalCoords other) {
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
