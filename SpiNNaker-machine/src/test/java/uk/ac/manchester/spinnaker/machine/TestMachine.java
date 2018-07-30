/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
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

    Router ROUTER = new Router(Arrays.asList(
            link00_01, link01_11, link11_20, link10_30));

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
                chips.add(new Chip(x, y, processors, ROUTER, SDRAM, address,
                        false, 0, BOOT_CHIP));
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
                new MachineDimensions(5, 5), chips, BOOT_CHIP);

        assertEquals(4, instance.maxChipX());
        assertEquals(4, instance.maxChipY());

        for (Chip c:instance.chips()) {
            assertEquals(address, c.ipAddress);
            assertEquals(SDRAM, c.sdram);
            assertEquals(ROUTER, c.router);
            for (Processor p: c.processors()) {
                assertThat(processors, hasItems(p));
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
        assertEquals("[Machine: max_x=4, max_y=4, n_chips=25]", instance.toString());
        assertFalse(instance.spinnakerLinks().iterator().hasNext());
    }

    @Test
    public void testRepeatChipInvalid() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = createdChips(processors);
        chips.add(new Chip(
                0, 0, processors, ROUTER, SDRAM, null, false, 0, BOOT_CHIP));
        assertThrows(IllegalArgumentException.class, () -> {
            Machine instance = new Machine(
                    new MachineDimensions(5, 5), chips, BOOT_CHIP);
        });
    }

    @Test
    public void testAddChip() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = new ArrayList();
        Machine instance = new Machine(
                new MachineDimensions(5, 5), chips, BOOT_CHIP);
        Chip chip00 = new Chip(
                0, 0, processors, ROUTER, SDRAM, null, false, 0, BOOT_CHIP);
        instance.addChip(chip00);
        assertEquals(1, instance.nChips());
        Chip repeat = new Chip(
                0, 0, processors, ROUTER, SDRAM, null, false, 0, BOOT_CHIP);
        assertThrows(IllegalArgumentException.class, () -> {
            instance.addChip(repeat);
        });
        Chip outOfRange = new Chip(
                5, 5, processors, ROUTER, SDRAM, null, false, 0, BOOT_CHIP);
        assertThrows(IllegalArgumentException.class, () -> {
            instance.addChip(repeat);
        });
        assertEquals(1, instance.nChips());
    }

    @Test
    public void testRepeatAdd() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = createdChips(processors);
        Machine instance = new Machine(
                new MachineDimensions(5, 5), chips, BOOT_CHIP);
        assertThrows(IllegalArgumentException.class, () -> {
            instance.addChip(new Chip(
                0, 0, processors, ROUTER, SDRAM, null, false, 0, BOOT_CHIP));
        });
    }

    @Test
    public void testChipAt() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = createdChips(processors);
        Machine instance = new Machine(
                new MachineDimensions(5, 5), chips, BOOT_CHIP);
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
                new MachineDimensions(5, 5), chips, BOOT_CHIP);
        assertEquals(processors.size() -1, instance.maximumUserCoresOnChip());
        instance.reserveSystemProcessors();
        assertEquals(processors.size() - 2, instance.maximumUserCoresOnChip());
    }

    @Test
    public void testMachineGetChipsOnBoard() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = createdChips(processors);
        Machine instance = new Machine(
                new MachineDimensions(5, 5), chips, BOOT_CHIP);
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
        Chip chip = new Chip(
                23, 23, processors, ROUTER, SDRAM, null, false, 0, BOOT_CHIP);
        instance.addChip(chip);
        assertEquals(chip,
                instance.getChipOverLink(chip00, Direction.SOUTHWEST));
    }
}
