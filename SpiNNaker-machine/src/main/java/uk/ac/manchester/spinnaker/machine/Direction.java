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
package uk.ac.manchester.spinnaker.machine;

/**
 * A direction from one SpiNNaker chip to another.
 *
 * @author Christian-B
 */
public enum Direction {
	/** Direction 0 typically towards a location x + 1, y. */
	EAST(0, +1, 0, "east"),
	/** Direction 1 typically towards a location x + 1, y +1. */
	NORTHEAST(1, +1, +1, "north_east"),
	/** Direction 2 typically towards a location x, y +1. */
	NORTH(2, 0, +1, "north"),
	/** Direction 3 typically towards a location x - 1, y. */
	WEST(3, -1, 0, "west"),
	/** Direction 4 typically towards a location x -1, y -1. */
	SOUTHWEST(4, -1, -1, "south_west"),
	/** Direction 5 typically towards a location x, y -1. */
	SOUTH(5, 0, -1, "south");

	private static final Direction[] BY_ID = {
		EAST, NORTHEAST, NORTH, WEST, SOUTHWEST, SOUTH
	};

	/** The ID of this direction when it is expressed as an {@code int}. */
	public final int id;

	/**
	 * The typical change to X when moving in this direction.
	 */
	public final int xChange;

	/**
	 * The typical change to Y when moving in this direction.
	 */
	public final int yChange;

	/**
	 * The string representation, for example used in JSON.
	 */
	public final String label;

	/**
	 * @param id
	 *            ID of this direction.
	 * @param xChange
	 *            Typical change to X if moving in this direction.
	 * @param yChange
	 *            Typical change to Y if moving in this direction.
	 */
	Direction(int id, int xChange, int yChange, String label) {
		this.id = id;
		this.xChange = xChange;
		this.yChange = yChange;
		this.label = label;
	}

	/**
	 * Obtains the inverse direction.
	 *
	 * @return The inverse direction.
	 */
	Direction inverse() {
		switch (this) {
		case EAST:
			return WEST;
		case NORTHEAST:
			return SOUTHWEST;
		case NORTH:
			return SOUTH;
		case WEST:
			return EAST;
		case SOUTHWEST:
			return NORTHEAST;
		default:
			return NORTH;
		}
	}

	/**
	 * The Direction with this ID when expressed as an {@code int}.
	 *
	 * @param id
	 *            ID of this Direction
	 * @return Direction with this ID
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the ID is not correct
	 */
	public static Direction byId(int id) throws ArrayIndexOutOfBoundsException {
		if (id < 0 || id >= BY_ID.length) {
			throw new ArrayIndexOutOfBoundsException("direction ID " + id
					+ " not in range 0 to " + (BY_ID.length - 1));
		}
		return BY_ID[id];
	}

	/**
	 * The Direction with this label
	 * <p>
	 * The current implementation assumes the labels are lower-case and words
	 * are separated by underscore (e.g., {@code north_east}).
	 *
	 * @param label
	 *            Label of a direction
	 * @return A valid direction
	 * @throws IllegalArgumentException
	 *             If no direction is found.
	 */
	public static Direction byLabel(String label) {
		for (var direction : Direction.values()) {
			if (direction.label.equals(label)) {
				return direction;
			}
		}
		throw new IllegalArgumentException(
				"No direction found for \"" + label + "\"");
	}
}
