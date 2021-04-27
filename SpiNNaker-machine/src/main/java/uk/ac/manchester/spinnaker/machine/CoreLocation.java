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
     * @param x
     *            The X coordinate, in range 0..255
     * @param y
     *            The Y coordinate, in range 0..255
     * @param p
     *            The P coordinate, in range 0..17
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
     * @param chip
     *            The X and Y coordinate, in range 0..255
     * @param p
     *            The P coordinate, in range 0..17
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
        return (((x << MachineDefaults.COORD_SHIFT)
                ^ y) << MachineDefaults.CORE_SHIFT) ^ p;
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
