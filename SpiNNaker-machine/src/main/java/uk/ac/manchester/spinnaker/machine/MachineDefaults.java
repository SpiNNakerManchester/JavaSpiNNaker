/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

/**
 *
 * @author Christian-B
 */
public final class MachineDefaults {

    private MachineDefaults() { }

    /** Clock speed in MHz of a standard Processor. */
    public static final int PROCESSOR_CLOCK_SPEED = 200 * 1000 * 1000;

    /** DTCM available on each standard Processor. */
    public static final int DTCM_AVAILABLE = 65536; // 2 ** 16;

    /** Standard number of Processors on each Chip. */
    public static final int PROCESSORS_PER_CHIP = 18;

    /** Entries available on a standard Router. */
    public static final int ROUTER_AVAILABLE_ENTRIES = 1024;

    /** Clock speed in MHz of a standard Router. */
    public static final int ROUTER_CLOCK_SPEED = 150 * 1024 * 1024;

    /** Max links available on a standard Router. */
    public static final int MAX_LINKS_PER_ROUTER = 6;

    /** Max x coordinate for a chip regardless of the type of machine. */
    public static final int MAX_X = 255;

    /** Max y coordinate for a chip regardless of the type of machine. */
    public static final int MAX_Y = 255;

    /**
      * Width of field of hashcode for holding (one dimension of the) chip
      * cooordinate.
      */
    public static final int COORD_SHIFT = 8;

    /** The maximum number of cores present on a chip. */
    static final int MAX_NUM_CORES = 18;

    /** Width of field of hashcode for holding processor ID. */
    static final int CORE_SHIFT = 5;

    /**
     * Checks the x and y parameter are legal ones
     *    regardless of the type of machine.
     *
     * @param x X part of the chips location
     * @param y Y part of the chips location
     * @throws IllegalArgumentException
     */
    public static void validateChipLocation(int x, int y)
            throws IllegalArgumentException {
        if (x < 0 || x > MAX_X) {
        	throw new IllegalArgumentException("bad X cooordinate");
        }
        if (y < 0 || y > MAX_Y) {
        	throw new IllegalArgumentException("bad Y cooordinate");
        }
    }

    /**
     * Checks the x,  y and p, parameter are legal ones
     *    regardless of the type of machine.
     *
     * @param x X part of the core/chip's location
     * @param y Y part of the core/chip's location
     * @param p P part of the core's location
     * @throws IllegalArgumentException
     */
    public static void validateCoreLocation(int x, int y, int p)
            throws IllegalArgumentException {
        validateChipLocation(x, y);
        if (p < 0 || p >= MAX_NUM_CORES) {
            throw new IllegalArgumentException("bad processor ID");
        }
    }



}
