/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

/**
 *
 * @author Christian-B
 */
public enum Direction {

    EAST(0),
    NORTHEAST(1),
    NORTH(2),
    WEST(3),
    SOUTHWEST(4),
    SOUTH(5);

    private static final Direction[] BY_ID =
        {EAST, NORTHEAST, NORTH, WEST, SOUTHWEST, SOUTH};

    public final int id;

    private Direction(int id) {
        this.id = id;
    }

    public static Direction byId(int id){
        return BY_ID[id];
    }

}
