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
package uk.ac.manchester.spinnaker.machine;

/**
 *
 * @author Christian-B
 */
public enum Direction {

    /** Direction 0 typically towards a location x + 1, y.  */
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

    private static final Direction[] BY_ID =
        {EAST, NORTHEAST, NORTH, WEST, SOUTHWEST, SOUTH};

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

    /**
     * Constructs an element of the Enum.
     *
     * @param id
     *            ID of this Direction.
     * @param xChange
     *            Typical change to X if moving in this Direction.
     * @param yChange
     *            Typical change to Y if moving in this Direction.
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
     * The Direction with this ID when expressed as an int.
     *
     * @param id
     *            ID of this Direction
     * @return Direction with this ID
     * @throws ArrayIndexOutOfBoundsException
     *             if the ID is not correct
     */
    public static Direction byId(int id) throws ArrayIndexOutOfBoundsException {
        return BY_ID[id];
    }

    /**
     * The Direction with this label
     * <p>
     * The current implementation assumes the labels are lowercase and words are
     * separated by underscore.
     *
     * @param label
     *            Label of this Direction
     * @return IllegalArgumentException If no direction is found.
     */
    public static Direction byLabel(String label) {
        for (Direction direction: Direction.values()) {
            if (direction.label.equals(label)) {
                return direction;
            }
        }
        throw new IllegalArgumentException(
                "No direction found for \"" + label + "\"");
    }
}
