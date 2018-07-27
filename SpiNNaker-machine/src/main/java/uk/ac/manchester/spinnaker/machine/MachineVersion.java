/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

/**
 *
 * @author Christian-B
 */
public enum MachineVersion {

    /** Original style 4 chip board. */
    TWO(2, true),
    /** Common style 4 chip board. */
    THREE(3, true),
    /** Original style 48 chip board. */
    FOUR(4, false),
    /** Single common style 48 chip board. */
    FIVE(5, false),
    /** Combination of common style 48 chips board in a Toroid. */
    TRIAD_WITH_WRAPAROUND(null, true),
    /**
     * Combination of common style 48 chips board not in a Toroid,
     *      but made up of Triads.
     */
    TRIAD_NO_WRAPAROUND(null, false),
    /**
     * Unexpected combination of common style 48 chips board not in a Toroid,
     *      nor made up of standard Triads.
     */
    NONE_TRIAD_LARGE(null, false),
    /**
     * Machine with an unexpected width or height.
     */
    INVALID(null, false);

    /** Python Id for this version or null if no matching id in python. */
    public final Integer id;

    /** Indicates if this board is expected to have wrap arounds. */
    public final boolean wrapAround;

    /**
     * Constructor.
     * @param id Python version id if a single board or null if not.
     * @param wrapAround Specifies if the board has a wrap around links.
     */
    MachineVersion(Integer id, boolean wrapAround) {
        this.id = id;
        this.wrapAround = wrapAround;
    }

    /**
     * Converts a python board id into a MachineVersion.
     *
     * @param id Python board version.
     * @return Machine version assuming just a single board.
     */
    public static MachineVersion byId(Integer id) {
       for (MachineVersion possible: MachineVersion.values()) {
           if (possible.id == id) {
               return possible;
           }
       }
       throw new IllegalArgumentException("No Board version with id: " + id);
    }

}
