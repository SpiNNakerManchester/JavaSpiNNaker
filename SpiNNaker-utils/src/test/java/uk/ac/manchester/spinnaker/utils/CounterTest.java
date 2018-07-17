/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class CounterTest {

    public CounterTest() {
    }

    /**
     * Test of increment and get methods, of class Counter.
     */
    @Test
    public void testIncrement() {
        Counter counter = new Counter();
        assertEquals(0, counter.get());
        counter.increment();
        counter.increment();
        assertEquals(2, counter.get());
    }

}
