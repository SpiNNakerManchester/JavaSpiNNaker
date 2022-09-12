/*
 * Copyright (c) 2018-2019 The University of Manchester
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
import static java.lang.Integer.parseUnsignedInt;

import java.util.regex.Pattern;

import javax.validation.constraints.PositiveOrZero;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * A simple description of a BMP to talk to. Supports equality and being used as
 * a hash key.
 * <p>
 * Although every board technically has a BMP, they are managed at the level of
 * a frame (when a sufficient quantity of boards is used, typically but not
 * necessarily 24). Cabinets contain frames.
 */
public final class BMPCoords implements Comparable<BMPCoords> {
	/** The ID of the cabinet that contains the frame that contains the BMPs. */
	@PositiveOrZero(message = "cabinet number must not be negative")
	private final int cabinet;

	/**
	 * The ID of the frame that contains the master BMP. Frames are contained
	 * within a cabinet.
	 */
	@PositiveOrZero(message = "frame number must not be negative")
	private final int frame;

	/**
	 * Create an instance.
	 *
	 * @param cabinet
	 *            The ID of the cabinet that contains the frame that contains
	 *            the BMPs.
	 * @param frame
	 *            The ID of the frame that contains the master BMP. Frames are
	 *            contained within a cabinet.
	 */
	public BMPCoords(int cabinet, int frame) {
		this.cabinet = cabinet;
		this.frame = frame;
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
		cabinet = parseUnsignedInt(m.group(++idx));
		frame = parseUnsignedInt(m.group(++idx));
	}

	/**
	 * @return The ID of the cabinet that contains the frame that contains the
	 *         BMPs.
	 */
	public int getCabinet() {
		return cabinet;
	}

	/**
	 * @return The ID of the frame that contains the master BMP. Frames are
	 *         contained within a cabinet.
	 */
	public int getFrame() {
		return frame;
	}

	@Override
	public int hashCode() {
		return cabinet << 16 | frame;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof BMPCoords) {
			var b = (BMPCoords) o;
			return cabinet == b.cabinet && frame == b.frame;
		}
		return false;
	}

	@Override
	public String toString() {
		return "[c:" + cabinet + ",f:" + frame + "]";
	}

	@Override
	public int compareTo(BMPCoords other) {
		int cmp = compare(cabinet, other.cabinet);
		if (cmp != 0) {
			return cmp;
		}
		return compare(frame, other.frame);
	}
}
