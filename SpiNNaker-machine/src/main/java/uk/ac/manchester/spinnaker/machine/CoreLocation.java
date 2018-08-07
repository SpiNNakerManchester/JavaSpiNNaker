/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

/**
 *
 * @author alan
 * @author dkf
 */
public final class CoreLocation
        implements HasCoreLocation, Comparable<CoreLocation> {
    private final int x;
    private final int y;
    private final int p;

    /**
     * Create the location of a core on a SpiNNaker machine.
     *
     * @param x The X coordinate, in range 0..255
     * @param y The Y coordinate, in range 0..255
     * @param p The P coordinate, in range 0..17
     */
    public CoreLocation(int x, int y, int p) {
        MachineDefaults.validateCoreLocation(x, y, p);
        this.x = x;
        this.y = y;
        this.p = p;
    }

    /**
     * Create the location of a core on a SpiNNaker machine.
     *
     * @param chip The X and Y coordinate, in range 0..255
     * @param p The P coordinate, in range 0..17
     */
    public CoreLocation(HasChipLocation chip, int p) {
        this(chip.getX(), chip.getY(), p);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CoreLocation)) {
            return false;
        }
        CoreLocation that = (CoreLocation) obj;
        return (this.x == that.x) && (this.y == that.y) && (this.p == that.p);
    }

    @Override
    public int hashCode() {
        return (((x << MachineDefaults.COORD_SHIFT) ^ y)
                << MachineDefaults.CORE_SHIFT) ^ p;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getP() {
        return p;
    }

    @Override
    public String toString() {
        return "X:" + getX() + " Y:" + getY() + " P:" + getP();
    }

    @Override
    public CoreLocation asCoreLocation() {
        return this;
    }

    @Override
    public int compareTo(CoreLocation o) {
        if (this.x < o.x) {
            return -1;
        } else if (this.x > o.x) {
            return 1;
        }
        if (this.y < o.y) {
            return -1;
        } else if (this.y > o.y) {
            return 1;
        }
        if (this.p < o.p) {
            return -1;
        } else if (this.p > o.p) {
            return 1;
        }
        return 0;
    }
}
