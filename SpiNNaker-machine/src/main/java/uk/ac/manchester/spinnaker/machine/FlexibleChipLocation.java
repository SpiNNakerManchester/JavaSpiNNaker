/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

/**
 *
 * @author Christian-B
 */
public class FlexibleChipLocation extends ChipLocation {

    FlexibleChipLocation(int x, int y) {
        super(x, y);
    }

   /**
     * Checks the x and y parameter are legal ones
     *    regardless of the type of machine.
     *
     * @param x X part of the chips location
     * @param y Y part of the chips location
     * @throws IllegalArgumentException
     */
    void validateChipLocation(int x, int y) {
         // nothing
    }

}
