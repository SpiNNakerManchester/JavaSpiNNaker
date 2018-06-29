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

    /** The maximum value of any coordinate. */
    static final int MAX_COORD = 255;
	/**
	 * Width of field of hashcode for holding (one dimension of the) chip
	 * cooordinate.
	 */
    static final int COORD_SHIFT = 8;

    /**
     * Create the location of a chip on a SpiNNaker machine.
     *
     * @param x The X cooordinate, in range 0..255
     * @param y The Y cooordinate, in range 0..255
     */
    public ChipLocation(int x, int y) {
        this.x = x;
        this.y = y;
        if (x < 0 || x > MAX_COORD) {
        	throw new IllegalArgumentException("bad X cooordinate");
        }
        if (y < 0 || y > MAX_COORD) {
        	throw new IllegalArgumentException("bad Y cooordinate");
        }
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
        return (x << COORD_SHIFT) ^ y;
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
}
