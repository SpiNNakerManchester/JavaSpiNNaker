/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

/**
 *
 * @author alan
 * @author dkf
 */
public class ChipLocation implements HasChipLocation {
    private final int x;
    private final int y;

    public ChipLocation(int x, int y) {
        this.x = x;
        this.y = y;
        // TODO: validate x and y for physical sanity
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ChipLocation))
            return false;

        ChipLocation that = (ChipLocation) obj;
        return (this.x == that.x) && (this.y == that.y);
    }

    @Override
    public int hashCode() {
        return (x << 4) ^ y;
    }

    @Override
    public final int getX() {
        return x;
    }

    @Override
    public final int getY() {
        return y;
    }
}
