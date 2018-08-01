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
    TWO(2, 2, 2, true, false, true, false),
    /** Common style 4 chip board. */
    THREE(3, 2, 2, true, false, true, false),
    /** Original style 48 chip board. */
    FOUR(4, 8, 8, false, true, false, false),
    /** Single common style 48 chip board. */
    FIVE(5, 8, 8, false, true, false, false),
    /** Combination of common style 48 chips board in a Toroid. */
    THREE_BOARD(12, 12),
    TWENTYFOUR_BOARD(48, 24),
    ONE_TWENTY_BOARD(96, 60),
    SIX_HUNDRED_BOARD(240, 120),
    ONE_THOUSAND_TWO_HUNDRED_BOARD(240, 240),
    TRIAD_WITH_WRAPAROUND(true, true),
    /**
     * Combination of common style 48 chips board not in a Toroid,
     *      but made up of Triads.
     */
    TRIAD_NO_WRAPAROUND(false, true),
    /**
     * Unexpected combination of common style 48 chips board not in a Toroid,
     *      nor made up of standard Triads.
     */
    NONE_TRIAD_LARGE(false, false);

    /** Python Id for this version or null if no matching id in python. */
    public final Integer id;

    /** Indicates if this machine has exactly 4 chips is a 2 by 2 layout. */
    public final boolean isFourChip;

    /** Indicates if this machine has exactly one 48 chip board. */
    public final boolean isFourtyeightChip;

    /** Indicates if this machine is expected to have wrap arounds. */
    public final boolean wrapAround;

    /** Indicates if this board is made up of triads,
     *      ie one or more groups of three boards in the typical layout. */
    public final boolean isTriad;

    /**
     * The only possible dimensions for this version
     * or null if multiple sizes allowed.
     */
    public final MachineDimensions machineDimensions;

    /**
     * Constructor.
     * @param id Python version id if a single board or null if not.
     * @param wrapAround Specifies if the board has a wrap around links.
     */
    private MachineVersion(Integer id, MachineDimensions dimensions,
            boolean isFourChip, boolean isFourtyeightChip, boolean wrapAround,
            boolean isTriad) {
        this.id = id;
        this.wrapAround = wrapAround;
        this.machineDimensions = dimensions;
        this.isFourChip = isFourChip;
        this.isFourtyeightChip = isFourtyeightChip;
        this.isTriad = isTriad;
    }

    private MachineVersion(int width, int height) {
        this.id = null;
        this.machineDimensions = new MachineDimensions(width, height);
        this.isFourChip = false;
        this.isFourtyeightChip = false;
        if ((width % MachineDefaults.TRIAD_HEIGHT == 0)
                && (height % MachineDefaults.TRIAD_WIDTH == 0)) {
            this.wrapAround = true;
            this.isTriad = true;
        } else if (((width - MachineDefaults.HALF_SIZE)
                % MachineDefaults.TRIAD_HEIGHT == 0)
                && ((height - MachineDefaults.HALF_SIZE)
                        % MachineDefaults.TRIAD_WIDTH == 0)) {
            this.isTriad = true;
            this.wrapAround = false;
        } else {
            this.wrapAround = false;
            this.isTriad = false;
        }
    }

    private MachineVersion(Integer id, int width, int height,
            boolean isFourChip, boolean isFourtyeightChip, boolean wrapAround,
            boolean isTriad) {
        this(id, new MachineDimensions(width, height), isFourChip,
                isFourtyeightChip, wrapAround, isTriad);
    }

    private MachineVersion(boolean wrapAround, boolean isTriad) {
        this(null, null, false, false, wrapAround, isTriad);
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

    /**
     * Calculate the machine version based on the size.
     *
     * @param width
     *            The width of the machine to find the version for.
     * @param height
     *            The height of the machine to find the version for.
     * @return A Board version.
     * @throws IllegalArgumentException if the size is not valid.
     */
    public static MachineVersion bySize(int width, int height) {
        return bySize(new MachineDimensions(width, height));
    }

    /**
     * Calculate the machine version based on the size.
     *
     * @param dimensions
     *            The width and height of the machine to find the version for.
     * @return A Board version.
     * @throws IllegalArgumentException if the size is not valid.
     */
    public static MachineVersion bySize(MachineDimensions dimensions) {
        if (MachineVersion.THREE.machineDimensions.equals(dimensions)) {
            return MachineVersion.THREE;
        }
        if (MachineVersion.FIVE.machineDimensions.equals(dimensions)) {
            return MachineVersion.FIVE;
        }
        for (MachineVersion possible: MachineVersion.values()) {
           if (dimensions.equals(possible.machineDimensions)) {
               return possible;
           }
        }
        if ((dimensions.width % MachineDefaults.TRIAD_HEIGHT == 0)
                && (dimensions.height % MachineDefaults.TRIAD_WIDTH == 0)) {
            return MachineVersion.TRIAD_WITH_WRAPAROUND;
        }
        if (((dimensions.width - MachineDefaults.HALF_SIZE)
                % MachineDefaults.TRIAD_HEIGHT == 0)
                && ((dimensions.height - MachineDefaults.HALF_SIZE)
                        % MachineDefaults.TRIAD_WIDTH == 0)) {
            return MachineVersion.TRIAD_NO_WRAPAROUND;
        }
        // Having elimiated a single 4 chip and 48 chip board
        // as well as a three board troid we need at least two board
        // on top of each other or next to each other
        if (dimensions.width < MachineDefaults.SIZE_X_OF_ONE_BOARD *2) {
            if (dimensions.height < MachineDefaults.SIZE_Y_OF_ONE_BOARD * 2) {
                throw new IllegalArgumentException(
                    "Dimensions " + dimensions + "too small!");
            }
        }
        if (dimensions.width % MachineDefaults.HALF_SIZE == 0
                && dimensions.height % MachineDefaults.HALF_SIZE == 0) {
            return MachineVersion.NONE_TRIAD_LARGE;
        }
        throw new IllegalArgumentException(
            "Dimensions " + dimensions + "not possible with current boards!");
    }

}
