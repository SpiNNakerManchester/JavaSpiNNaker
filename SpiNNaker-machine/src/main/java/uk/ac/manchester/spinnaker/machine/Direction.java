/*
 * Copyright (c) 2018 The University of Manchester
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
     * The String representation for example used in json.
     */
    public final String label;

    /**
     * Constructs the Enum.
     * @param id ID of this Direction.
     * @param xChange Typical change to X if moving in this Direction.
     * @param yChange Typical change to Y if moving in this Direction.
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
            case EAST: return WEST;
            case NORTHEAST: return SOUTHWEST;
            case NORTH: return SOUTH;
            case WEST: return EAST;
            case SOUTHWEST: return NORTHEAST;
            default: return NORTH;
        }
    }

    /**
     * The Direction with this id when expressed as an int.
     *
     * @param id ID of this Direction
     * @return Direction with this id
     * @throws ArrayIndexOutOfBoundsException if the id is not correct
     */
    public static Direction byId(int id) throws ArrayIndexOutOfBoundsException {
        return BY_ID[id];
    }

    /**
     * The Direction with this label
     * <p>
     * The current implementation assumes the labels are lowercase and
     *      words are separated by underscore.
     *
     * @param label Label of this Direction
     * @return IllegalArgumentException If no direction is found.
     */
    public static Direction byLabel(String label) {
        for (Direction direction: Direction.values()) {
            if (direction.label.equals(label)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("No direction found for \"" + label + "\"");
    }

    /**
     * Computes the typical destination location when moving from source in
     *    this direction.
     * <p>
     * This helper method ONLY computes the typical move.
     * It does not check that the move is possible or correct.
     * The possibility of a wrap around is IGNORED!
     * There is no check if the destination location exists.
     * There is no check if the links is missing, dead or otherwise used.
     *
     * @param source Location moving from.
     * @return The typical location this direction goes to from this source.
     * @deprecated Not Sure this method will remain
     *      as it can not do the negative x and y
     */
    @Deprecated
    public ChipLocation typicalMove(HasChipLocation source) {
        return new ChipLocation(
            source.getX() + xChange, source.getY() + yChange);
    }

}
