/*
 * Copyright (c) 2014 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.machinemanager.responses;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Describes a chip by its X,Y location.
 */
@JsonPropertyOrder({"x", "y"})
@JsonFormat(shape = ARRAY)
public class Chip {
	/** The x-coordinate of the chip. */
	private int x;

	/** The y-coordinate of the chip. */
	private int y;

	/**
	 * Get the x-coordinate of the chip.
	 *
	 * @return The x-coordinate of the chip
	 */
	public int getX() {
		return x;
	}

	/**
	 * Set the x-coordinate of the chip.
	 *
	 * @param x
	 *            The x-coordinate of the chip
	 */
	public void setX(final int x) {
		this.x = x;
	}

	/**
	 * Get the y-coordinate of the chip.
	 *
	 * @return The y-coordinate of the chip
	 */
	public int getY() {
		return y;
	}

	/**
	 * Set the y-coordinate of the chip.
	 *
	 * @param y
	 *            The y-coordinate of the chip
	 */
	public void setY(final int y) {
		this.y = y;
	}
}
