/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

/**
 *
 * @author Christian-B
 */
public enum BufferingOperation {
    BUFFER_READ(0),
    BUFFER_WRITE(1);

    public final int value;

    BufferingOperation(int value) {
        this.value = value;
    }
    
}
