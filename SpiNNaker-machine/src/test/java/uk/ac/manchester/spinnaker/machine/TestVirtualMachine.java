/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.hamcrest.collection.IsEmptyCollection;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.datalinks.FPGALinkData;
import uk.ac.manchester.spinnaker.machine.datalinks.FpgaId;
import uk.ac.manchester.spinnaker.machine.datalinks.SpinnakerLinkData;

/**
 *
 * @author Christian-B
 */
public class TestVirtualMachine {

    @Test
    public void testSmallBoardsMini() {
        Machine instance = new VirtualMachine(new MachineDimensions(2, 2));
    }

    @Test
    public void testSmallBoards() {
        Machine instance = new VirtualMachine(new MachineDimensions(2, 2),
            new HashSet<ChipLocation>(),
            new HashMap<ChipLocation, Collection<Integer>>(),
            new HashMap<ChipLocation, Collection<Direction>>());
        assertEquals(MachineVersion.THREE, instance.version);
        assertEquals(4, instance.chips().size());
        for (Chip chip:instance.chips()) {
            if (MachineDefaults.FOUR_CHIP_DOWN_LINKS.
                    containsKey(chip.asChipLocation())) {
                Set<Direction> bad = MachineDefaults.FOUR_CHIP_DOWN_LINKS.
                        get(chip.asChipLocation());
                for (Link link:chip.router) {
                    assertThat(bad, not(hasItems(link.sourceLinkDirection)));
                }
            }
        }

        InetAddress address00 = instance.bootChip().ipAddress;
        assertNotNull(address00);

        instance.addFpgaLinks();
        ArrayList<FPGALinkData> fpgalinks = new ArrayList();
        instance.getFpgaLinks().forEach(fpgalinks::add);
        assertEquals(0, fpgalinks.size());

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
    public void testSingleBoard() throws UnknownHostException {
        Machine instance = new VirtualMachine(MachineVersion.FOUR);
        assertEquals(48, instance.chips().size());
               instance.addFpgaLinks();
        InetAddress address = instance.bootChip().ipAddress;

        assertEquals("864 cores and 120.0 links",
                instance.coresAndLinkOutputString());

        instance.addFpgaLinks();
        FPGALinkData link = instance.getFpgaLink(FpgaId.BOTTOM, 3, address);
        assertEquals(address, link.boardAddress);
        assertEquals(Direction.SOUTH, link.direction);
        assertEquals(FpgaId.BOTTOM, link.fpgaId);
        assertEquals(3, link.fpgaLinkId);

        ArrayList<FPGALinkData> links = new ArrayList();
        instance.getFpgaLinks().forEach(links::add);
        assertEquals(3 * 16, links.size());
    }

    @Test
    public void test3Boards() {
        Machine instance = new VirtualMachine(MachineVersion.THREE_BOARD);
        assertEquals(3 * 48, instance.chips().size());
        assertEquals(3 * 48 * 17, instance.totalAvailableUserCores());
        instance.reserveSystemProcessors();
        assertEquals(3 * 48 * 16, instance.totalAvailableUserCores());

        instance.addFpgaLinks();
        ArrayList<FPGALinkData> links = new ArrayList();
        instance.getFpgaLinks().forEach(links::add);
        assertEquals(0, links.size());
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
        assertFalse(instance.hasLinkAt(new ChipLocation(0, 0), Direction.SOUTHWEST));
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
        SpinnakerLinkData data00b = instance.getSpinnakerLink(0, null);
        assertEquals(data00, data00b);
    }

    @Test
    public void test3BoardWrappedWithFPGALinks() {
        Map<ChipLocation, Collection<Direction>> ignoreLinks = new HashMap();
        //Make room for fpga links with two none fpga ignores as well
        //South is a fpg NE is not
        ignoreLinks.put(new ChipLocation(0, 0),
                Arrays.asList(Direction.SOUTH, Direction.NORTHEAST));
        ignoreLinks.put(new ChipLocation(0, 3), Arrays.asList(Direction.WEST));
        ignoreLinks.put(new ChipLocation(7, 2), Arrays.asList(Direction.NORTH));
        // Middle of board so never fpga
        ignoreLinks.put(new ChipLocation(1, 1), Arrays.asList(Direction.NORTH));

        Machine instance = new VirtualMachine(new MachineDimensions(12, 12),
                null, null, ignoreLinks);
        // Only ignored in one direction so 2.5 less
        assertEquals("2592 cores and 429.5 links", instance.coresAndLinkOutputString());
        assertFalse(instance.hasLinkAt(new ChipLocation(7, 2), Direction.NORTH));
        instance.addFpgaLinks();
        ArrayList<FPGALinkData> links = new ArrayList();
        instance.getFpgaLinks().forEach(links::add);
        assertEquals(3, links.size());

    }

    @Test
    public void test3BoardNoWrap() throws UnknownHostException {
        Map<ChipLocation, Collection<Direction>> ignoreLinks = new HashMap();

        Machine instance = new VirtualMachine(new MachineDimensions(16, 16),
                null, null, ignoreLinks);
        assertEquals(3 * 48, instance.chips().size());

        instance.addFpgaLinks();
        ArrayList<FPGALinkData> links = new ArrayList();
        instance.getFpgaLinks().forEach(links::add);
        // 16 links per fpga
        // each board has 2 fpga open (one connected to other board)
        // There are three boards
        assertEquals(16 * 2 * 3, links.size());

        // Never fpga at the bbc internter address
        InetAddress bbc = InetAddress.getByName("151.101.128.81");
        assertNull(instance.getFpgaLink(FpgaId.BOTTOM, 0, bbc));
        assertFalse(instance.getFpgaLinks(bbc).iterator().hasNext());

        InetAddress bootAddress = instance.bootChip().ipAddress;

        // Never any addresses on the top right of the boot board
        assertNull(instance.getFpgaLink(FpgaId.TOP_RIGHT, 0, bootAddress));

        Iterator<FPGALinkData> iterator = instance.getFpgaLinks(bootAddress).iterator();
        int count = 0;
        while(iterator.hasNext()) {
            count++;
            iterator.next();
        }
        assertEquals(16 * 2, count);
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
        Machine instance = new VirtualMachine(new MachineDimensions(252,248));
        assertEquals(60528, instance.chips().size());
        assertEquals(MachineVersion.NONE_TRIAD_LARGE, instance.version);
    }

    @Test
    public void testCoverageHackery() {
        assertThrows(Error.class, () -> {
            VirtualMachine.addressFromBytes(new byte[0]);
        });
    }

}
