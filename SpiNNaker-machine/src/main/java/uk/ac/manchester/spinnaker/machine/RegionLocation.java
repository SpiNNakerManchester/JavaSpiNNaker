package uk.ac.manchester.spinnaker.machine;

/**
 * Holding case for a CoreLocation X, Y and P and the recording region.
 *
 * @author Christian
 */
public class RegionLocation implements HasCoreLocation,
        Comparable<RegionLocation> {

    /**
     * The Chip / Core's X value.
     */
    public final int x;
    /**
     * The Chip / Core's Y value.
     */
    public final int y;
    /**
     * The Core's P value.
     */
    public final int p;
    /**
     * The recording Region.
     */
    public final int region;
    /**
     * Precalculated hashcode.
     */
    public final int hashcode;

    /**
     * Creates the Region based on a Core and a region.
     * @param core The Core to use
     * @param region The Region to use.
     */
    public RegionLocation(HasCoreLocation core, int region) {
        this.x = core.getX();
        this.y = core.getY();
        this.p = core.getP();
        this.region = region;
        this.hashcode = ((((x << MachineDefaults.COORD_SHIFT) ^ y)
                << MachineDefaults.CORE_SHIFT) ^ p)
                << MachineDefaults.REGION_SHIFT ^ region;
    }

    @Override
    public int getP() {
        return p;
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
    public int compareTo(RegionLocation o) {
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
        if (this.region < o.region) {
            return -1;
        } else if (this.region > o.region) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RegionLocation)) {
            return false;
        }
        RegionLocation that = (RegionLocation) obj;
        return (this.x == that.x) && (this.y == that.y) && (this.p == that.p)
                && (this.region == that.region);
    }

    @Override
    public String toString() {
        return "X:" + x + " Y:" + y + " P:" + p + "R: " + region;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

}
