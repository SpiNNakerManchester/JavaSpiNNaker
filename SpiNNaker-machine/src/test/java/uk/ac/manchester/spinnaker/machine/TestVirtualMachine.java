/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian-B
 */
public class TestVirtualMachine {

    @Test
    public void testSmallBoards() {
        Machine instance = new VirtualMachine(new MachineDimensions(2, 2),
            new HashSet<ChipLocation>(),
            new HashMap<ChipLocation, Collection<Integer>>(),
            new HashMap<ChipLocation, Collection<Direction>>());
        assertEquals(4, instance.chips().size());
    }

    @Test
    public void testBad() {
        assertThrows(Exception.class, () -> {
            Machine instance = new VirtualMachine(
                new MachineDimensions(121, 120));
        });
     }

    @Test
    public void test3Boards() {
        Machine instance = new VirtualMachine(MachineVersion.THREE_BOARD);
        assertEquals(3 * 48, instance.chips().size());
    }

    @Test
    public void test24Boards() {
        Machine instance = new VirtualMachine(MachineVersion.TWENTYFOUR_BOARD);
        assertEquals(24 * 48, instance.chips().size());
    }

    @Test
    public void test120Boards() {
        Machine instance = new VirtualMachine(MachineVersion.ONE_TWENTY_BOARD);
        assertEquals(120 * 48, instance.chips().size());
    }

    @Test
    public void test600Boards() {
        Machine instance = new VirtualMachine(MachineVersion.SIX_HUNDRED_BOARD);
        assertEquals(600 * 48, instance.chips().size());
    }

    @Test
    public void test1200Boards() {
        Machine instance = new VirtualMachine(
                MachineVersion.ONE_THOUSAND_TWO_HUNDRED_BOARD);
        assertEquals(1200 * 48, instance.chips().size());
    }

    @Test
    public void testBiggestWrapAround() {
        Machine instance = new VirtualMachine(new MachineDimensions(252,252),
            new HashSet<ChipLocation>(),
            new HashMap<ChipLocation, Collection<Integer>>(),
            new HashMap<ChipLocation, Collection<Direction>>());
        assertEquals(252 * 252, instance.chips().size());
        assertEquals(MachineVersion.TRIAD_WITH_WRAPAROUND, instance.version);
    }

    @Test
    public void testBiggestNoneWrapAround() {
        Machine instance = new VirtualMachine(new MachineDimensions(244,244),
            new HashSet<ChipLocation>(),
            new HashMap<ChipLocation, Collection<Integer>>(),
            new HashMap<ChipLocation, Collection<Direction>>());
        assertEquals(57600, instance.chips().size());
        assertEquals(MachineVersion.TRIAD_NO_WRAPAROUND, instance.version);
    }

    @Test
    public void testBiggestWeird() {
        Machine instance = new VirtualMachine(new MachineDimensions(252,248),
            new HashSet<ChipLocation>(),
            new HashMap<ChipLocation, Collection<Integer>>(),
            new HashMap<ChipLocation, Collection<Direction>>());
        assertEquals(60528, instance.chips().size());
        assertEquals(MachineVersion.NONE_TRIAD_LARGE, instance.version);
    }
}
