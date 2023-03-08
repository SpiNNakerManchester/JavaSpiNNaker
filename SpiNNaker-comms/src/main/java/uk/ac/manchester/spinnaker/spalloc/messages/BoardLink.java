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
