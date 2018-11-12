/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.storage_objects;

/**
 * output buffering operations.
 *
 * a listing of what SpiNNaker specific EIEIO commands there are.
 *
 * @see <a href=
 * "https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon/blob/master/spinn_front_end_common/utilities/constants.py">
 * Python Version</a>
 *
 * @author Christian-B
 */
public enum BufferingOperation {
    /** Database handshake with external program. */
    BUFFER_READ(0),
    /** Host confirming data being read form SpiNNaker memory. */
    BUFFER_WRITE(1);

    /** Python value for this enum. */
    public final int value;

    BufferingOperation(int value) {
        this.value = value;
    }

}
