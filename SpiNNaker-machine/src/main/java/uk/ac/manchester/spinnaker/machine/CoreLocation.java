/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

/**
 *
 * @author alan
 * @author dkf
 */
public class CoreLocation implements HasCoreLocation {
    private final int x;
    private final int y;
    private final int p;

    public CoreLocation(int x, int y, int p) {
        this.x = x;
        this.y = y;
        this.p = p;
        // TODO make this validation aware of SpiNNaker 2
        if (p < 0 || p >= 18) {
            throw new IllegalArgumentException("bad processor ID");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof CoreLocation))
            return false;

        CoreLocation that = (CoreLocation) obj;
        return (this.x == that.x) && (this.y == that.y) && (this.p == that.p);
    }

    @Override
    public int hashCode() {
        return (((x << 4) ^ y) << 4) ^ p;
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
    public final int getP() {
        return p;
    }
}
