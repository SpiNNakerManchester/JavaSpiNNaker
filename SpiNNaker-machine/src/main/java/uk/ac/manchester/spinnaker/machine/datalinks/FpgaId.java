/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.datalinks;

/**
 *
 * @author Christian-B
 */
public enum FpgaId {

    /** The FGPA link that connects to the bottom and bottom right chips. */
    BOTTOM(0),
    /** The FGPA link that connects to the left and top left chips. */
    LEFT(1),
    /** The FGPA link that connects to the top and right chips. */
    TOP_RIGHT(2);

    /** The physical id for this link. */
    public final int id;

    /**
     * Converts an Id into an enum.
     */
    private static final FpgaId[] BY_ID =
        {BOTTOM, LEFT, TOP_RIGHT};

    FpgaId(int id) {
        this.id = id;
    }

    /**
     * Obtain the enum from the id.
     *
     * @param id The physical id for the FPGA link.
     * @return The id as an enum
     * @throws ArrayIndexOutOfBoundsException
     *      Thrown if the ID is outside the known range.
     */
    public static FpgaId byId(int id) throws ArrayIndexOutOfBoundsException {
        return BY_ID[id];
    }
}
