package uk.ac.manchester.spinnaker.machine;

/** Represents the size of a machine in chips. */
public final class MachineDimensions {
    /** The width of the machine in chips. */
    public final int width;
    /** The height of the machine in chips. */
    public final int height;

    /**
     * Create a new instance.
     *
     * @param width
     *            The width of the machine, in chips.
     * @param height
     *            The height of the machine, in chips.
     */
    public MachineDimensions(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        return (o != null) && (o instanceof MachineDimensions)
                && equals((MachineDimensions) o);
    }

    /**
     * Tests whether this object is equal to another dimension.
     *
     * @param dimension
     *            The other dimension object to compare to.
     * @return True exactly when they are equal.
     */
    public boolean equals(MachineDimensions dimension) {
        if (dimension == null) {
            return false;
        }
        return width == dimension.width && height == dimension.height;
    }

    @Override
    public int hashCode() {
        return width << 16 | height;
    }

    @Override
    public String toString() {
        return "Width:" + width + " Height:" + height;
    }
}
