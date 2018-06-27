/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Christian-B
 */
public class TestProcessor {
    @Test
    public void testProcessorBasicUse() {
        Processor p1 = Processor.factory(1);
        Processor p2 = Processor.factory(2);
        assertNotEquals(p1, p2);
        Processor p1f = Processor.factory(1, false);
        assertEquals(p1, p1f);
    }
}
