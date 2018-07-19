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
public class TestMachineVersion {

    public TestMachineVersion() {
    }

    @Test
    public void testById() {
        assertEquals(MachineVersion.FIVE, MachineVersion.byId(5));
        assertThrows(IllegalArgumentException.class, () -> {
            MachineVersion.byId(1);
        });
     }


}
