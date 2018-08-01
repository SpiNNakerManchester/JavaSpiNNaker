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

    @Test
    public void testThree() {
        MachineVersion instance = MachineVersion.THREE;
        assertEquals(new MachineDimensions(2,2), instance.machineDimensions);
    }

    @Test
    public void testMachineVersion() {
        assertEquals(MachineVersion.THREE, MachineVersion.bySize(
                new MachineDimensions(2, 2)));
        assertEquals(MachineVersion.FIVE, MachineVersion.bySize(8, 8));
        assertEquals(MachineVersion.THREE_BOARD,
                MachineVersion.bySize(12, 12));
        assertEquals(MachineVersion.TRIAD_NO_WRAPAROUND, MachineVersion.bySize(
                new MachineDimensions(16, 16)));
        assertEquals(MachineVersion.NONE_TRIAD_LARGE, MachineVersion.bySize(
                new MachineDimensions(20, 20)));
        assertEquals(MachineVersion.TRIAD_WITH_WRAPAROUND,
                MachineVersion.bySize(12+24, 12+36));
        assertEquals(MachineVersion.TRIAD_NO_WRAPAROUND, MachineVersion.bySize(
                new MachineDimensions(16+24, 16+36)));
        assertEquals(MachineVersion.NONE_TRIAD_LARGE, MachineVersion.bySize(
                new MachineDimensions(20+24, 20+36)));
        assertEquals(MachineVersion.NONE_TRIAD_LARGE,  MachineVersion.bySize(
                new MachineDimensions(12, 16)));
        assertThrows(IllegalArgumentException.class, () -> {
            MachineVersion.bySize(new MachineDimensions(13, 16));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MachineVersion.bySize(new MachineDimensions(12, 4));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MachineVersion.bySize(8, 12);
        });
    }

}
