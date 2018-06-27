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
    public static final int CLOCK_SPEED = 200 * 1000 * 1000;

    /** DTCM available on each standard Processor. */
    public static final int DTCM_AVAILABLE = 65536; // 2 ** 16;

    /** Standard number of Processors on each Chip. */
    public static final int PROCESSORS_PER_CHIP = 18;
}
