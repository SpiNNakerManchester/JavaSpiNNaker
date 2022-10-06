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

/**
 * A simple description of a BMP to talk to. Supports equality and being used as
 * a hash key.
 * <p>
 * Although every board technically has a BMP, they are managed at the level of
 * a frame (when a sufficient quantity of boards is used, typically but not
 * necessarily 24). Cabinets contain frames.
 *
 * @author Donal Fellows
 */
public final class BMPCoords implements Comparable<BMPCoords> {
	/** Parses the result of {@link #toString()}. */
	private static final Pattern PATTERN =
			Pattern.compile("^\\[c:(\\d+),f:(\\d+)\\]$");

	/** The ID of the cabinet that contains the frame that contains the BMPs. */
	@ValidCabinetNumber
	public final int c; // TODO rename

	/**
	 * The ID of the frame that contains the master BMP. Frames are contained
	 * within a cabinet.
	 */
	@ValidFrameNumber
	public final int f; // TODO rename

	/**
	 * Create an instance.
	 *
	 * @param cabinet
	 *            Cabinet number.
	 * @param frame
	 *            Frame number.
	 */
	public BMPCoords(int cabinet, int frame) {
		this.c = cabinet;
		this.f = frame;
	}

	/**
	 * Create an instance from its serial form. This is the form produced by
	 * {@link #toString()}. The serial form (where the numbers may vary) is:
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
			throw new IllegalArgumentException("bad argument: " + serialForm);
		}
		int idx = 0;
		c = parseInt(m.group(++idx));
		f = parseInt(m.group(++idx));
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
		return c * 5 + f;
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
