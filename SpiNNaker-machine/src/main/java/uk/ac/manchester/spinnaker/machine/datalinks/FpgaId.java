/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.datalinks;

/**
 *
 * @author Christian-B
 */
public enum FpgaId {
    BOTTOM(0),
    LEFT(1),
    TOP_RIGHT(2);

    public final int id;

    private static final FpgaId[] BY_ID =
        {BOTTOM, LEFT, TOP_RIGHT};

    FpgaId(int id) {
        this.id = id;
    }

    public static FpgaId byId(int id) throws ArrayIndexOutOfBoundsException {
        return BY_ID[id];
    }

}
