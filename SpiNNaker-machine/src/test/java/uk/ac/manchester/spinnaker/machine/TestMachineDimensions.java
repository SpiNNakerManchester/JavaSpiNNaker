/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Christian-B
 */
public class TestMachineDimensions {

    public TestMachineDimensions() {
    }

    @Test
    public void testEquals() {
       MachineDimensions m1 = new MachineDimensions(2, 3);
       MachineDimensions m2 = new MachineDimensions(2, 3);
       assertEquals(m1, m2);
       assertEquals(m1.hashCode(), m2.hashCode());
       assertEquals(m1.toString(), m2.toString());
    }

    @Test
    public void testNullEquals() {
       MachineDimensions m1 = new MachineDimensions(2, 3);
       MachineDimensions m2 = null;
       assertNotEquals(m1, m2);
       assertFalse(m1.equals(m2));
    }
}
