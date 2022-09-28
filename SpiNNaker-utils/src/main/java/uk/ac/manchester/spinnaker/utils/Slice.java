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
package uk.ac.manchester.spinnaker.utils;

import static java.util.Objects.nonNull;

import com.google.errorprone.annotations.Immutable;

/**
 * A description of a slice (range) of an object. Modelled after the concept
 * with the same name in Python. Note that this does not create the actual
 * range; it merely <i>describes</i> it.
 *
 * @author Donal Fellows
 */
@Immutable
public final class Slice {
	/** The index where the slice starts. */
	public final Integer start;

	/**
	 * The index where the slice stops. (One after the last accessible byte.)
	 */
	public final Integer stop;

	private Slice(Integer start, Integer stop) {
		this.start = start;
		this.stop = stop;
	}

	/**
	 * Create a new slice from the start position to the end of the IO object.
	 *
	 * @param start
	 *            Where to start.
	 * @return The slice
	 */
	public static Slice from(int start) {
		return new Slice(start, null);
	}

	/**
	 * Create a new slice from the beginning to the stop position of the IO
	 * object.
	 *
	 * @param end
	 *            Where to finish.
	 * @return The slice
	 */
	public static Slice to(int end) {
		return new Slice(null, end);
	}

	/**
	 * Create a new slice, from the the start position to the stop position, of
	 * the IO object.
	 *
	 * @param start
	 *            Where to start.
	 * @param end
	 *            Where to finish.
	 * @return The slice
	 */
	public static Slice over(int start, int end) {
		return new Slice(start, end);
	}

	@Override
	public String toString() {
		var str = new StringBuilder("Slice(");
		if (nonNull(start)) {
			str.append(start);
		}
		str.append(";");
		if (nonNull(stop)) {
			str.append(stop);
		}
		return str.append(")").toString();
	}
}
