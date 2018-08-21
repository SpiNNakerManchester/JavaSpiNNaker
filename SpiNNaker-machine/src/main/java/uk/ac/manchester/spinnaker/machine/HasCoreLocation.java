/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

/**
 *
 * @author dkf
 */
public interface HasCoreLocation extends HasChipLocation {
    /**
     * @return The processor coordinate of the core on its chip.
     */
    int getP();

    /**
     * Check if two locations are colocated at the core level. This does
     * <i>not</i> imply that the two are equal.
     *
     * @param other
     *            The other location to compare to.
     * @return If the two locations have the same X, Y and P coordinates.
     */
    default boolean onSameCoreAs(HasCoreLocation other) {
        return onSameChipAs(other) && (getP() == other.getP());
    }

    /**
     * Converts (if required) this to a simple X, Y, P tuple.
     *
     * @return A CoreLocation representation of the X, Y, P tuple
     */
    default CoreLocation asCoreLocation()  {
        return new CoreLocation(getX(), getY(), getP());
    }
}
