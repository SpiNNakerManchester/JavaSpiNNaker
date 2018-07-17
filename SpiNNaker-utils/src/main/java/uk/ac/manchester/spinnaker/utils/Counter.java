/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

/**
 * Thin wrapper around an int for Counting.
 * <p>
 * This allows the Object to be final and therefor passed into inner classes.
 * <p>
 * This is NOT thread safe.
 *
 * @author Christian-B
 */
public class Counter {

    private int count;

    /**
     * Create a counter starting at zero.
     */
    public Counter() {
        count = 0;
    }

    /**
     * Add one to the count.
     */
    public void increment() {
        count+= 1;
    }

    /**
     * Retrieve the current value.
     *
     * @return The current counter value.
     */
    public int get() {
        return count;
    }
}
