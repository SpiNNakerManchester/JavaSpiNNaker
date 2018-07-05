/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

/**
 *
 * @author Christian-B
 */
public enum BoardVersion {

    TWO(2, true),
    THREE(3, true),
    FOUR(4, false),
    FIVE(5, false),
    TOROID(null, true),
    LARGE(null, false),
    INVALID(null, false);

    public final Integer id;
    public final boolean wrapAround;

    BoardVersion (Integer id, boolean wrapAround){
        this.id = id;
        this.wrapAround = wrapAround;
    }

    public static BoardVersion byId(Integer id) {
       for (BoardVersion possible: BoardVersion.values()) {
           if (possible.id == id) {
               return possible;
           }
       }
       throw new IllegalArgumentException("No Board version with id: " +id);
    }

    public static BoardVersion bySize(int width, int height){
       if ((width == 2) && (height == 2)) {
           return THREE;
       }
       if ((width == 8) && (height == 8)) {
           return FIVE;
       }
       if ((width % 12 == 0) && (height % 12 == 0)) {
           return TOROID;
       }
       if (((width -4 ) % 12 == 0) && ((height -4) % 12 == 0)) {
           return LARGE;
       }
       return INVALID;
    }

}
