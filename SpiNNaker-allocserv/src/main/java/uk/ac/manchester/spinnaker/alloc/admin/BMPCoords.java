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

/**
 * Frame/BMP coordinates.
 *
 * @author Donal Fellows
 */
public final class BMPCoords // FIXME
		implements Comparable<BMPCoords> {
	/** Cabinet number. */
	@PositiveOrZero(message = "cabinet number must not be negative")
	public final int c;

	/** Frame number. */
	@PositiveOrZero(message = "frame number must not be negative")
	public final int f;

	/**
	 * Create an instance.
	 *
	 * @param c
	 *            Cabinet number.
	 * @param f
	 *            Frame number.
	 */
	public BMPCoords(int c, int f) {
		this.c = c;
		this.f = f;
	}

	private static final Pattern PATTERN =
			Pattern.compile("^\\[c:(\\d+),f:(\\d+)\\]$");

	/**
	 * Create an instance from its serial form. The serial form (where the
	 * numbers may vary) is:
	 *
	 * <pre>
	 * [c:34,f:12]
	 * </pre>
	 *
	 * @param serialForm
	 *            The form to deserialise.
	 * @throws IllegalArgumentException
	 *             If the string is not in the right form.
	 */
	@JsonCreator
	public BMPCoords(String serialForm) {
		var m = PATTERN.matcher(serialForm);
		if (!m.matches()) {
			throw new IllegalArgumentException(
					"bad argument: " + serialForm);
		}
		int idx = 0;
		c = parseDec(m.group(++idx));
		f = parseDec(m.group(++idx));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BMPCoords) {
			var other = (BMPCoords) obj;
			return c == other.c && f == other.f;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return ((c << 2 + c) ^ f) << 2 + f;
	}

	@Override
	public String toString() {
		return "[c:" + c + ",f:" + f + "]";
	}

	@Override
	public int compareTo(BMPCoords other) {
		int cmp = compare(c, other.c);
		if (cmp != 0) {
			return cmp;
		}
		return compare(f, other.f);
	}
}
