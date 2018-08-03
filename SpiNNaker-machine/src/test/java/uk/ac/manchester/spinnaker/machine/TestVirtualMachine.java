/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.hamcrest.collection.IsEmptyCollection;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.datalinks.SpinnakerLinkData;

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
        for (Chip chip:instance.chips()) {
            if (MachineDefaults.FOUR_CHIP_DOWN_LINKS.
                    containsKey(chip.asChipLocation())) {
                Set<Direction> bad = MachineDefaults.FOUR_CHIP_DOWN_LINKS.
                        get(chip.asChipLocation());
                for (Link link:chip.router.links()) {
                    assertThat(bad, not(hasItems(link.sourceLinkDirection)));
                }
            }
        }

        InetAddress address00 = instance.bootChip().ipAddress;
        assertNotNull(address00);

        Collection<SpinnakerLinkData> empty = instance.spinnakerLinks();
        assertThat(empty, IsEmptyCollection.empty());

        instance.addSpinnakerLinks();
        Collection<SpinnakerLinkData> links = instance.spinnakerLinks();
        assertEquals(2, links.size());
        for (SpinnakerLinkData link:links) {
            assertEquals(address00, link.boardAddress);
        }
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
    public void testSpinnakerLinks() {
        Map<ChipLocation, Collection<Direction>> ignoreLinks = new HashMap();
        ignoreLinks.put(new ChipLocation(0, 0), Arrays.asList(Direction.SOUTHWEST));
        ignoreLinks.put(new ChipLocation(8, 4), Arrays.asList(Direction.SOUTHWEST));
        ignoreLinks.put(new ChipLocation(4, 8), Arrays.asList(Direction.SOUTHWEST));
        Machine instance = new VirtualMachine(new MachineDimensions(12, 12),
                null, null, ignoreLinks);
        assertEquals(3 * 48, instance.chips().size());
        assertEquals(3 * 48 * 17, instance.totalAvailableUserCores());
        // Only ignored in one direction so 1.5 less
        assertEquals("2592 cores and 430.5 links", instance.coresAndLinkOutputString());
        Collection<SpinnakerLinkData> empty = instance.spinnakerLinks();
        assertThat(empty, IsEmptyCollection.empty());
        instance.addSpinnakerLinks();
        Collection<SpinnakerLinkData> links = instance.spinnakerLinks();
        assertEquals(3, links.size());
        for (SpinnakerLinkData link:links) {
            assertEquals(Direction.SOUTHWEST, link.direction);
            assertEquals(0, link.spinnakerLinkId);
            assertNotNull(link.boardAddress);
        }
        InetAddress address84 = instance.getChipAt(8, 4).ipAddress;
        assertNotNull(address84);
        SpinnakerLinkData data84 = instance.getSpinnakerLink(0, address84);
        assertEquals(Direction.byId(4), data84.direction);
        assertEquals(address84, data84.boardAddress);
        assertEquals(0, data84.spinnakerLinkId);
        InetAddress address00 = instance.bootChip().ipAddress;
        SpinnakerLinkData data00 = instance.getBootSpinnakerLink(0);
        assertEquals(address00, data00.boardAddress);
        SpinnakerLinkData data00a = instance.getSpinnakerLink(0, address00);
        assertEquals(data00, data00a);
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
