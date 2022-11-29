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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadX;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadY;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;

/**
 * The logical coordinates of a board. This would be {@link TriadCoords} except
 * it has a different serialization form for backward-compatibility.
 */
@JsonPropertyOrder({
	"x", "y", "z"
})
@JsonFormat(shape = ARRAY)
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
@Immutable
public final class BoardCoordinates {
	@ValidTriadX
	private final int x;

	@ValidTriadY
	private final int y;

	@ValidTriadZ
	private final int z;

	/**
	 * Create with given coordinates.
	 *
	 * @param x
	 *            the X coordinate
	 * @param y
	 *            the Y coordinate
	 * @param z
	 *            the Z coordinate
	 */
	public BoardCoordinates(
			@JsonProperty(value = "x", defaultValue = "0") int x,
			@JsonProperty(value = "y", defaultValue = "0") int y,
			@JsonProperty(value = "z", defaultValue = "0") int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Create with given coordinates.
	 *
	 * @param triad
	 *            the coordinates in standard form
	 */
	public BoardCoordinates(TriadCoords triad) {
		this.x = triad.x;
		this.y = triad.y;
		this.z = triad.z;
	}

	/** @return the X coordinate */
	public int getX() {
		return x;
	}

	/** @return the Y coordinate */
	public int getY() {
		return y;
	}

	/** @return the Z coordinate */
	public int getZ() {
		return z;
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof BoardCoordinates other) && (x == other.x)
				&& (y == other.y) && (z == other.z);
	}

	@Override
	public int hashCode() {
		return x * 1234567 + y * 56789 + z;
	}

	@Override
	public String toString() {
		return "Board@(" + x + "," + y + "," + z + ")";
	}

	/**
	 * Convert to the standard coordinate scheme.
	 *
	 * @return the coordinates
	 */
	public TriadCoords toStandardCoords() {
		return new TriadCoords(x, y, z);
	}
}
