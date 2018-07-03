/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

/**
 * The location of a Chip as an X and Y tuple.
 * <p>
 * This class is final as it is used a key in maps.
 *
 * @author alan
 * @author dkf
 */
public final class ChipLocation implements HasChipLocation, Comparable<ChipLocation> {
    private final int x;
    private final int y;

    /**
     * Create the location of a chip on a SpiNNaker machine.
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     */
    public ChipLocation(int x, int y) {
        MachineDefaults.validateChipLocation(x, y);
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChipLocation)) {
            return false;
        }
        ChipLocation that = (ChipLocation) obj;
        return (this.x == that.x) && (this.y == that.y);
    }

    @Override
    public int hashCode() {
        return (x << MachineDefaults.COORD_SHIFT) ^ y;
    }

    @Override
    public int compareTo(ChipLocation o) {
        if (this.x < o.x) {
            return -1;
        }
        if (this.x > o.x) {
            return 1;
        }
        if (this.y < o.y) {
            return -1;
        }
        if (this.y > o.y) {
            return 1;
        }
        return 0;
    }

    @Override
    public final int getX() {
        return x;
    }

    @Override
    public final int getY() {
        return y;
    }

    @Override
    public String toString() {
        return "X:" + getX() + " Y:" + getY();
    }

    @Override
    public ChipLocation asChipLocation() {
        return this;
    }

 }
