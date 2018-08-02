/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
        assertEquals(3 * 48 * 17, instance.totalAvailableUserCores());
        instance.reserveSystemProcessors();
        assertEquals(3 * 48 * 16, instance.totalAvailableUserCores());
        assertEquals("2592 cores and 432.0 links", instance.coresAndLinkOutputString());
    }

    @Test
    public void testNullIgnores() {
        Machine instance = new VirtualMachine(new MachineDimensions(12, 12),
                null, null, null);
        assertEquals(3 * 48, instance.chips().size());
        assertEquals(3 * 48 * 17, instance.totalAvailableUserCores());
        assertEquals("2592 cores and 432.0 links", instance.coresAndLinkOutputString());
    }

    @Test
    public void testIgnoresLinks() {
        Map<ChipLocation, Collection<Direction>> ignoreLinks = new HashMap();
        ignoreLinks.put(new ChipLocation(7, 7), Arrays.asList(Direction.NORTH));
        Machine instance = new VirtualMachine(new MachineDimensions(12, 12),
                null, null, ignoreLinks);
        assertEquals(3 * 48, instance.chips().size());
        assertEquals(3 * 48 * 17, instance.totalAvailableUserCores());
        // Only ignored in one direction so .5 less
        assertEquals("2592 cores and 431.5 links", instance.coresAndLinkOutputString());
    }

    @Test
    public void testIgnoreCores() {
        Map<ChipLocation, Collection<Integer>> ignoreCores = new HashMap();
        ignoreCores.put(new ChipLocation(7, 7), Arrays.asList(3, 5, 7));
        Machine instance = new VirtualMachine(new MachineDimensions(12, 12),
                null, ignoreCores, null);
        assertEquals(3 * 48, instance.chips().size());
        Chip chip = instance.getChipAt(7, 7);
        assertEquals(14, chip.nUserProcessors());
        assertEquals(3 * 48 * 17 - 3, instance.totalAvailableUserCores());
    }

    @Test
    public void testIgnoreChips() {
        Set<ChipLocation> ignoreChips = new HashSet();
        ignoreChips.add(new ChipLocation(4,4));
        ignoreChips.add(new ChipLocation(9,10));
        Machine instance = new VirtualMachine(new MachineDimensions(12, 12),
                ignoreChips, null, null);
        assertEquals(3 * 48 - 2 , instance.chips().size());
    }

    @Test
    public void testIgnoreRootChips() {
        Set<ChipLocation> ignoreChips = new HashSet();
        ignoreChips.add(new ChipLocation(8, 4));
        // Note future Machine may disallow a null ethernet chip
        Machine instance = new VirtualMachine(new MachineDimensions(12, 12),
                ignoreChips, null, null);
        // Note future VirtualMachines may ignore the whole board!
        assertEquals(3 * 48 -1 , instance.chips().size());
        Chip chip = instance.getChipAt(2, 9);
        assertEquals(new ChipLocation(8, 4), chip.nearestEthernet.asChipLocation());
        assertNull(instance.getChipAt(chip.nearestEthernet));
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
