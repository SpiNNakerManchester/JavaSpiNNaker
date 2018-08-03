/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Christian-B
 */
public class TestMachine {

    private ArrayList<Processor> processors;

    public TestMachine() {
    }

    ChipLocation chip00 = new ChipLocation(0, 0);
    ChipLocation chip01 = new ChipLocation(0, 1);
    ChipLocation chip10 = new ChipLocation(1, 0);
    ChipLocation chip11 = new ChipLocation(1, 1);
    ChipLocation chip20 = new ChipLocation(2, 0);
    ChipLocation chip30 = new ChipLocation(2, 0);

    Link link00_01 = new Link(chip00, Direction.NORTH, chip01);
    Link link01_11 = new Link(chip01, Direction.SOUTH, chip11);
    Link link11_20 = new Link(chip11, Direction.EAST, chip20);
    Link link10_30 = new Link(chip10, Direction.WEST, chip01);

    List<Link> LINKS = Arrays.asList(
            link00_01, link01_11, link11_20, link10_30);

    Router ROUTER = new Router(LINKS);

    int SDRAM = 100;
    ChipLocation BOOT_CHIP = chip00;

    byte[] bytes = {(byte)192, (byte)162, (byte)240, (byte)253};

    private ArrayList<Processor> createProcessors() {
        ArrayList<Processor> processors = new ArrayList();
        processors.add(Processor.factory(0));
        processors.add(Processor.factory(1));
        processors.add(Processor.factory(2));
        processors.add(Processor.factory(3, true));
        for (int i = 4; i < 18; i++) {
            processors.add(Processor.factory(i));
        }
        return processors;
    }

    private ArrayList<Chip> createdChips(ArrayList<Processor> processors) throws UnknownHostException {
        InetAddress address = InetAddress.getByAddress(bytes);
        ArrayList<Chip> chips = new ArrayList();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                chips.add(new Chip(new ChipLocation(x, y), processors, ROUTER,
                        SDRAM, address, false, 0, BOOT_CHIP));
            }
        }
        return chips;
    }

    @Test
    public void testCreateNewMachine() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = createdChips(processors);
        InetAddress address = InetAddress.getByAddress(bytes);

        Machine instance = new Machine(
                new MachineDimensions(8, 8), chips, BOOT_CHIP);

        assertEquals(7, instance.maxChipX());
        assertEquals(7, instance.maxChipY());

        for (Chip c:instance.chips()) {
            assertEquals(address, c.ipAddress);
            assertEquals(SDRAM, c.sdram);
            assert(c.router.links().containsAll(LINKS));
            for (Processor p: c.allProcessors()) {
                assertThat(processors, hasItems(p));
            }
            for (Processor p: c.monitorProcessors()) {
                assertTrue(p.isMonitor);
            }
            for (Processor p: c.userProcessors()) {
                assertFalse(p.isMonitor);
            }
        }

        assertEquals(450, instance.totalCores());
        assertEquals(425, instance.totalAvailableUserCores());
        assertEquals(ChipLocation.ZERO_ZERO, instance.boot);
        assertEquals(address, instance.bootChip().ipAddress );
        assertEquals(25, instance.nChips());
        // Not implemented as Java has no len and size() could be boards, chips, processors ect so a bad call anyway
        //self.assertEqual(len(new_machine), 25)
        // Not implemented as Java has no iter and iter() could be boards, chips, processors ect so a bad call anyway
        // self.assertEqual(next(x[1].ip_address for x in new_machine), self._ip)
        assertEquals(ChipLocation.ZERO_ZERO,
                instance.chipCoordinates().iterator().next());
        // String is simplified to assumje each link unique and bi directional
        assertEquals("450 cores and 50.0 links", instance.coresAndLinkOutputString());
        assertEquals("[Machine: max_x=7, max_y=7, n_chips=25]", instance.toString());
        assertFalse(instance.spinnakerLinks().iterator().hasNext());
        int count = 0;
        ChipLocation previous = null;
        for (Chip found:instance) {
            count++;
            if (previous != null) {
                assertThat(previous, lessThan(found.asChipLocation()));            }
            previous = found.asChipLocation();
        }
        assertEquals(25, count);
        SortedMap<ChipLocation, Chip> all = instance.chipsMap();
        assertEquals(25, all.size());
        assertFalse(instance.hasChipAt(null));
    }

    @Test
    public void testRepeatChipInvalid() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = createdChips(processors);
        chips.add(new Chip(ChipLocation.ZERO_ZERO, processors, ROUTER,
                        SDRAM, null, false, 0, BOOT_CHIP));
        assertThrows(IllegalArgumentException.class, () -> {
            Machine instance = new Machine(
                    new MachineDimensions(8, 8), chips, BOOT_CHIP);
        });
    }

    @Test
    public void testAddChip() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = new ArrayList();
        Machine instance = new Machine(
                new MachineDimensions(8, 8), chips, BOOT_CHIP);
        Chip chip00 = new Chip(ChipLocation.ZERO_ZERO, processors, ROUTER,
                        SDRAM, null, false, 0, BOOT_CHIP);
        instance.addChip(chip00);
        assertEquals(1, instance.nChips());
        Chip repeat = new Chip(ChipLocation.ZERO_ZERO, processors, ROUTER,
                        SDRAM, null, false, 0, BOOT_CHIP);
        assertThrows(IllegalArgumentException.class, () -> {
            instance.addChip(repeat);
        });
        Chip outOfRange1 = new Chip(new ChipLocation(5, 11), processors,
                ROUTER, SDRAM, null, false, 0, BOOT_CHIP);
        assertThrows(IllegalArgumentException.class, () -> {
            instance.addChip(outOfRange1);
        });
        Chip outOfRange2 = new Chip(new ChipLocation(11, 5), processors,
                ROUTER, SDRAM, null, false, 0, BOOT_CHIP);
        assertThrows(IllegalArgumentException.class, () -> {
            instance.addChip(outOfRange2);
        });
        assertEquals(1, instance.nChips());
    }

    @Test
    public void testLinks() {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = new ArrayList();
        Machine instance = new Machine(
                new MachineDimensions(8, 8), chips, BOOT_CHIP);
        Link link01 = new Link(chip00, Direction.NORTH, chip01);
        Link link10 = new Link(chip00, Direction.EAST, chip10);

        Router router = new Router(Arrays.asList(link01, link10));
        Chip chip00 = new Chip(ChipLocation.ZERO_ZERO, processors, router,
                        SDRAM, null, false, 0, BOOT_CHIP);
        //Chip created but not added
        assertFalse(instance.hasChipAt(ChipLocation.ZERO_ZERO));
        assertFalse(instance.hasLinkAt(ChipLocation.ZERO_ZERO, Direction.NORTH));

        instance.addChip(chip00);
        //Chip added
        assertTrue(instance.hasChipAt(ChipLocation.ZERO_ZERO));
        assertTrue(instance.hasLinkAt(ChipLocation.ZERO_ZERO, Direction.NORTH));
        assertFalse(instance.hasLinkAt(ChipLocation.ZERO_ZERO, Direction.SOUTH));
        assertFalse(instance.hasLinkAt(ChipLocation.ZERO_ZERO, null));
    }

    @Test
    public void testRepeatAdd() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = createdChips(processors);
        Machine instance = new Machine(
                new MachineDimensions(8, 8), chips, BOOT_CHIP);
        assertThrows(IllegalArgumentException.class, () -> {
            instance.addChip(new Chip(ChipLocation.ZERO_ZERO, processors,
                    ROUTER, SDRAM, null, false, 0, BOOT_CHIP));
        });
    }

    @Test
    public void testChipAt() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = createdChips(processors);
        Machine instance = new Machine(
                new MachineDimensions(8, 8), chips, BOOT_CHIP);
        assertEquals(chips.get(0), instance.getChipAt(ChipLocation.ZERO_ZERO));
        assertNull(instance.getChipAt(10, 10));
        assertTrue(instance.hasChipAt(ChipLocation.ZERO_ZERO));
        assertFalse(instance.hasChipAt(10, 10));
    }

    @Test
    public void testReserveSystemProcessor() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = createdChips(processors);
        Machine instance = new Machine(
                new MachineDimensions(8, 8), chips, BOOT_CHIP);
        assertEquals(processors.size() -1, instance.maximumUserCoresOnChip());
        instance.reserveSystemProcessors();
        assertEquals(processors.size() - 2, instance.maximumUserCoresOnChip());
    }

    @Test
    public void testMachineGetChipsOnBoard() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = createdChips(processors);
        Machine instance = new Machine(
                new MachineDimensions(8, 8), chips, BOOT_CHIP);
        int count = 0;
        for (Chip chip:instance.iterChipsOnBoard(chips.get(3))) {
            count++;
        }
        //DOes not include 0.4 as it is not on the board
        assertEquals(24, count);
    }

    @Test
    public void testGetChipOverLink() {
        Machine instance = new Machine(new MachineDimensions(24, 24),
                new ArrayList<Chip>(), BOOT_CHIP);
        ArrayList<Processor> processors = createProcessors();
        Chip chip =new Chip(new ChipLocation(23, 23), processors,
                ROUTER, SDRAM, null, false, 0, BOOT_CHIP);
        instance.addChip(chip);
        assertEquals(chip,
                instance.getChipOverLink(chip00, Direction.SOUTHWEST));
    }

    @Test
    public void testNormalizeWithWrapAround() {
        Machine instance = new Machine(new MachineDimensions(48, 24),
                new ArrayList<Chip>(), ChipLocation.ZERO_ZERO);
        assertEquals(new ChipLocation(24, 0), instance.normalizedLocation(24, 24));
        assertEquals(new ChipLocation(24, 0),
                instance.normalizedLocation(new ChipLocation(24, 24)));
    }

    @Test
    public void testNormalizeWithOutWrapAround() {
        Machine instance = new Machine(new MachineDimensions(52, 28),
                new ArrayList<Chip>(), ChipLocation.ZERO_ZERO);
        assertEquals(new ChipLocation(24, 24), instance.normalizedLocation(24, 24));
        assertEquals(new ChipLocation(24, 24),
                instance.normalizedLocation(new ChipLocation(24, 24)));
    }

    @Test
    public void testEthernetChip() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = new ArrayList();
        byte[] bytes00 = {127, 0, 0, 0};
        Chip chip00 = new Chip(ChipLocation.ZERO_ZERO, processors, null,
                        SDRAM, InetAddress.getByAddress(bytes00), false,
                0, BOOT_CHIP);
        chips.add(chip00);
        byte[] bytes84 = {127, 0, 8, 4};
        Chip chip84 = new Chip(new ChipLocation(8, 4), processors, null,
                        SDRAM, InetAddress.getByAddress(bytes84), false,
                0, BOOT_CHIP);
        chips.add(chip84);
        Chip chip01 = new Chip(new ChipLocation(0, 1), processors, null,
                        SDRAM, null, false,
                0, BOOT_CHIP);
        chips.add(chip01);
        Machine instance = new Machine(
                new MachineDimensions(12, 12), chips, BOOT_CHIP);
        assertEquals(3, instance.nChips());
        assertThat(instance.ethernetConnectedChips(),
                containsInAnyOrder(chip00, chip84));

        byte[] bytes48 = {127, 0, 4, 8};
        Chip chip48 = new Chip(new ChipLocation(4, 8), processors, null,
                        SDRAM, InetAddress.getByAddress(bytes48), false,
                0, BOOT_CHIP);
        instance.addChip(chip48);
        Chip chip02 = new Chip(new ChipLocation(0, 2), processors, null,
                        SDRAM, null, false,
                0, BOOT_CHIP);
        instance.addChip(chip02);
        assertEquals(5, instance.nChips());
        assertThat(instance.ethernetConnectedChips(),
                containsInAnyOrder(chip00, chip84, chip48));
    }


}
