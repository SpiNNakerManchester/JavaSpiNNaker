/*
 * Copyright (c) 2018-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine;

import static uk.ac.manchester.spinnaker.utils.CollectionUtils.makeEnumBackingMap;

import java.util.Map;

import uk.ac.manchester.spinnaker.machine.board.BoardDirection;

/**
 * A direction from one SpiNNaker chip to another.
 * <p>
 * Note that the numbers chosen have two useful properties:
 *
 * <ul>
 * <li>The integer values assigned are chosen to match the numbers used to
 * identify the links in the low-level software API and hardware registers.
 * <li>The links are ordered consecutively in anticlockwise order meaning the
 * opposite link is {@code (link+3)%6}.
 * </ul>
 * Note that the new Spalloc internally uses a {@linkplain BoardDirection
 * different notation} for link directions.
 *
 * @author Christian-B
 * @author Donal Fellows
 */
public enum Direction {

	/** Direction 0 typically towards a location x + 1, y. */
	EAST(0, +1, 0, "east", BoardDirection.SE),
	/** Direction 1 typically towards a location x + 1, y +1. */
	NORTHEAST(1, +1, +1, "north_east", BoardDirection.E),
	/** Direction 2 typically towards a location x, y +1. */
	NORTH(2, 0, +1, "north", BoardDirection.N),
	/** Direction 3 typically towards a location x - 1, y. */
	WEST(3, -1, 0, "west", BoardDirection.NW),
	/** Direction 4 typically towards a location x -1, y -1. */
	SOUTHWEST(4, -1, -1, "south_west", BoardDirection.W),
	/** Direction 5 typically towards a location x, y -1. */
	SOUTH(5, 0, -1, "south", BoardDirection.S);

	private static final Direction[] BY_ID = {
		EAST, NORTHEAST, NORTH, WEST, SOUTHWEST, SOUTH
	};

	/** The Id of this direction when it is expressed as an Integer. */
	public final int id;

	/**
	 * The typical change to x when moving in this direction.
	 */
	public final int xChange;

	/**
	 * The typical change to x when moving in this Direction.
	 */
	public final int yChange;

	/**
	 * The String representation for example used in JSON.
	 */
	public final String label;

	private final BoardDirection d;

	private static final Map<BoardDirection, Direction> MAP =
			makeEnumBackingMap(values(), v -> v.d);

	/**
	 * Constructs an element of the Enum.
	 *
	 * @param id
	 *            ID of this Direction.
	 * @param xChange
	 *            Typical change to X if moving in this Direction.
	 * @param yChange
	 *            Typical change to Y if moving in this Direction.
	 * @param d
	 *            The corresponding board direction.
	 */
	Direction(int id, int xChange, int yChange, String label,
			BoardDirection d) {
		this.id = id;
		this.xChange = xChange;
		this.yChange = yChange;
		this.label = label;
		this.d = d;
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
	 * The Direction with this ID when expressed as an int.
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
	 * The current implementation assumes the labels are lowercase and words are
	 * separated by underscore (e.g., {@code north_east}).
	 *
	 * @param label
	 *            Label of a Direction
	 * @return A valid Direction
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

	/**
	 * Get the inter-chip direction corresponding to a board direction.
	 *
	 * @param direction
	 *            The direction from a board.
	 * @return The inter-chip direction that corresponds.
	 */
	public static Direction of(BoardDirection direction) {
		return MAP.get(direction);
	}
}
