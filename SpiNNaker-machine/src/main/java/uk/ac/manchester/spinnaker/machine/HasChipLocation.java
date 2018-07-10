/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

/**
 *
 * @author dkf
 */
public interface HasChipLocation {
    /**
     * @return The X coordinate of the chip.
     */
    int getX();

    /**
     * @return The X coordinate of the chip.
     */
    int getY();

    /**
     * Check if two locations are colocated at the chip level. This does
     * <i>not</i> imply that the two are equal.
     *
     * @param other
     *            The other location to compare to.
     * @return If the two locations have the same X and Y coordinates.
     */
    default boolean onSameChipAs(HasChipLocation other) {
        return (getX() == other.getX()) && (getY() == other.getY());
    }

    /**
     * Get the core of the chip that will be running SC&MP. This is always core
     * 0 of the chip, as the core that runs SC&MP always maps itself to be
     * virtual core ID 0.
     *
     * @return The location of the SC&MP core.
     */
    default HasCoreLocation getScampCore() {
        // SCAMP always runs on core 0 or we can't talk to the chip at all
        return new CoreLocation(getX(), getY(), 0);
    }
}
