/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
		if (o instanceof BoardCoordinates) {
			var other = (BoardCoordinates) o;
			return x == other.x && y == other.y && z == other.z;
		}
		return false;
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
