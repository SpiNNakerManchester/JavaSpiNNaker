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

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.board.ValidTriadX;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadY;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;

/**
 * A combination of x, y, z and a Link.
 *
 * @author Christian
 */
@JsonPropertyOrder({
	"x", "y", "z", "link"
})
@JsonFormat(shape = ARRAY)
@Immutable
public class BoardLink {
	// TODO verify format and meaning.

	@ValidTriadX
	private final int x;

	@ValidTriadY
	private final int y;

	@ValidTriadZ
	private final int z;

	private final int link;

	/**
	 * @param x
	 *            The X coordinate
	 * @param y
	 *            The Y coordinate
	 * @param z
	 *            The Z coordinate
	 * @param link
	 *            The link number
	 */
	public BoardLink(@JsonProperty("x") int x, @JsonProperty("y") int y,
			@JsonProperty("z") int z, @JsonProperty("link") int link) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.link = link;
	}

	/**
	 * @return the X coordinate
	 */
	public int getX() {
		return x;
	}

	/**
	 * @return the Y coordinate
	 */
	public int getY() {
		return y;
	}

	/**
	 * @return the Z coordinate
	 */
	public int getZ() {
		return z;
	}

	/**
	 * @return the link number
	 */
	public int getLink() {
		return link;
	}
}
