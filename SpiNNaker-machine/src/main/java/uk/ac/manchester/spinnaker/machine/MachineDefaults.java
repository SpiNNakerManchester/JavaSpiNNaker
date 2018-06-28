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

    /** MAx links available on a standard Router. */
    public static final int MAX_LINKS_PER_ROUTER = 6;

}
