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
public final class Counter {

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
     * Add any amount to the counter.
     * <p>
     * Could also be used to add a negative number.
     *
     * @param other int values by which to change the counter.
     */
    public void add(int other){
        count += other;
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
