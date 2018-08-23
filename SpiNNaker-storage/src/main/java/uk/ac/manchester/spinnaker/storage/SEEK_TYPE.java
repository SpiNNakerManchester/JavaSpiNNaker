/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.storage;

/**
 *
 * @author Christian-B
 */
public enum SEEK_TYPE {
    /** absolute buffer positioning. */
    SEEK_SET,
    /**  seek relative to the current read. */
    SEEK_READ,
    /**  seek relative to the current write. */
    SEEK_WRITE,
    /** relative to the buffer's end. */
    SEEK_END;

}
