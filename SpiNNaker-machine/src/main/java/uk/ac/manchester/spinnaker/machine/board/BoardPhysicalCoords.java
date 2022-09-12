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

import static java.lang.Integer.compare;
import static java.lang.Integer.parseUnsignedInt;

import java.util.regex.Pattern;

import javax.validation.constraints.PositiveOrZero;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Physical board coordinates.
 *
 * @author Donal Fellows
 */
public final class BoardPhysicalCoords
		implements Comparable<BoardPhysicalCoords> {
	/** Cabinet number. */
	@PositiveOrZero(message = "cabinet number must not be negative")
	public final int cabinet;

	/** Frame number. */
	@PositiveOrZero(message = "frame number must not be negative")
	public final int frame;

	/** Board number. */
	@PositiveOrZero(message = "board number must not be negative")
	public final int board;

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
	public BoardPhysicalCoords(@JsonProperty("cabinet") int c,
			@JsonProperty("frame") int f, @JsonProperty("board") int b) {
		this.cabinet = c;
		this.frame = f;
		this.board = b;
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
		cabinet = parseUnsignedInt(m.group(++idx));
		frame = parseUnsignedInt(m.group(++idx));
		board = parseUnsignedInt(m.group(++idx));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BoardPhysicalCoords) {
			var other = (BoardPhysicalCoords) obj;
			return (cabinet == other.cabinet) && (frame == other.frame)
					&& (board == other.board);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (((cabinet << 2 + cabinet) ^ frame) << 2 + frame) ^ board;
	}

	@Override
	public String toString() {
		return "[c:" + cabinet + ",f:" + frame + ",b:" + board + "]";
	}

	/** @return The managing BMP address. */
	@JsonIgnore
	public BMPCoords getBmp() {
		return new BMPCoords(cabinet, frame);
	}

	/** @return The board number handle. */
	@JsonIgnore
	public BMPBoard getBoardNumber() {
		return new BMPBoard(board);
	}

	@Override
	public int compareTo(BoardPhysicalCoords other) {
		int cmp = compare(cabinet, other.cabinet);
		if (cmp != 0) {
			return cmp;
		}
		cmp = compare(frame, other.frame);
		if (cmp != 0) {
			return cmp;
		}
		return compare(board, other.board);
	}
}
