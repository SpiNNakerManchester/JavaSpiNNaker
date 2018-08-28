/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

/**
 * Thin wrapper around an <tt>int</tt> for counting.
 * <p>
 * This allows the object to be final and therefore passed into inner classes.
 * <p>
 * This is <i>not</i> thread safe.
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
        count++;
    }

    /**
     * Add any amount to the counter.
     * <p>
     * Could also be used to add a negative number.
     *
     * @param other int values by which to change the counter.
     */
    public void add(int other) {
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
