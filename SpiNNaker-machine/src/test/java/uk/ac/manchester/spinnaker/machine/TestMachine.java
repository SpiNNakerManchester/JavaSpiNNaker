/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import uk.ac.manchester.spinnaker.utils.DefaultMap;

/**
 *
 * @author Christian-B
 */
@SuppressWarnings("deprecation")
public class TestMachine {

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
        ArrayList<Processor> processors = new ArrayList<>();
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
        ArrayList<Chip> chips = new ArrayList<>();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                chips.add(new Chip(new ChipLocation(x, y), processors, ROUTER,
                        SDRAM, address, BOOT_CHIP));
            }
        }
        return chips;
    }

    private Router createRouter(
            ChipLocation source, Collection<ChipLocation> all) {
        ArrayList<Link> links = new ArrayList<>();
        for (Direction direction: Direction.values()) {
            int dest_x = source.getX() + direction.xChange;
            int dest_y = source.getY() + direction.yChange;
            if (dest_x >= 0 && dest_y >= 0) {
                ChipLocation destination = new ChipLocation(dest_x, dest_y);
                if (all.contains(destination)) {
                    links.add(new Link(
                        source, direction, new ChipLocation(dest_x, dest_y)));
                }
            }
        }
        return new Router(links);
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

        HasChipLocation hasLocation = null;
        assertFalse(instance.hasChipAt(hasLocation));
        hasLocation = new CoreLocation(3, 3, 2);
        assertTrue(instance.hasChipAt(hasLocation));
    }

    @Test
    public void testRepeatChipInvalid() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = createdChips(processors);
        chips.add(new Chip(ChipLocation.ZERO_ZERO, processors, ROUTER,
                        SDRAM, null, BOOT_CHIP));
        assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("unused")
            Machine instance = new Machine(
                    new MachineDimensions(8, 8), chips, BOOT_CHIP);
        });
    }

    @Test
    public void testAddChip() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = new ArrayList<>();
        Machine instance = new Machine(
                new MachineDimensions(8, 8), chips, BOOT_CHIP);
        Chip chip00 = new Chip(ChipLocation.ZERO_ZERO, processors, ROUTER,
                        SDRAM, null, BOOT_CHIP);
        instance.addChip(chip00);
        assertEquals(1, instance.nChips());
        Chip repeat = new Chip(ChipLocation.ZERO_ZERO, processors, ROUTER,
                        SDRAM, null, BOOT_CHIP);
        assertThrows(IllegalArgumentException.class, () -> {
            instance.addChip(repeat);
        });
        Chip outOfRange1 = new Chip(new ChipLocation(5, 11), processors,
                ROUTER, SDRAM, null, BOOT_CHIP);
        assertThrows(IllegalArgumentException.class, () -> {
            instance.addChip(outOfRange1);
        });
        Chip outOfRange2 = new Chip(new ChipLocation(11, 5), processors,
                ROUTER, SDRAM, null, BOOT_CHIP);
        assertThrows(IllegalArgumentException.class, () -> {
            instance.addChip(outOfRange2);
        });
        assertEquals(1, instance.nChips());
    }

    @Test
    public void testLinks() {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = new ArrayList<>();
        Machine instance = new Machine(
                new MachineDimensions(8, 8), chips, BOOT_CHIP);
        Link link01 = new Link(chip00, Direction.NORTH, chip01);
        Link link10 = new Link(chip00, Direction.EAST, chip10);

        Router router = new Router(Arrays.asList(link01, link10));
        Chip chip00 = new Chip(ChipLocation.ZERO_ZERO, processors, router,
                        SDRAM, null, BOOT_CHIP);
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
                    ROUTER, SDRAM, null, BOOT_CHIP));
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
        ArrayList<Processor> processors = new ArrayList<>();
        processors.add(Processor.factory(0, true));
        processors.add(Processor.factory(1, true));
        for (int i = 2; i < 18; i++) {
            processors.add(Processor.factory(i));
        }
        ArrayList<Chip> chips = new ArrayList<>();
        byte[] bytes00 = {127, 0, 0, 0};
        Chip chip00 = new Chip(ChipLocation.ZERO_ZERO, processors, null,
                        SDRAM, InetAddress.getByAddress(bytes00), BOOT_CHIP);
        chips.add(chip00);
        Chip chip01 = new Chip(new ChipLocation(0, 1), processors, null,
                        SDRAM, null, BOOT_CHIP);
        chips.add(chip01);
        Chip chip02 = new Chip(new ChipLocation(0, 2), Collections.emptySet(),
                null, SDRAM, null, BOOT_CHIP);
        chips.add(chip02);
        Machine instance = new Machine(
                new MachineDimensions(8, 8), chips, BOOT_CHIP);
        // Already 2 cores reserved.
        assertEquals(processors.size() - 2, instance.maximumUserCoresOnChip());
        assertEquals((processors.size() - 2 ) * 2 , instance.totalAvailableUserCores());
        assertEquals(processors.size() * 2, instance.totalCores());
    }

    @Test
    public void testMachineGetChipsOnBoard() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = createdChips(processors);
        Machine instance = new Machine(
                new MachineDimensions(8, 8), chips, BOOT_CHIP);
        int count = 0;
        for (@SuppressWarnings("unused") Chip chip:instance.iterChipsOnBoard(chips.get(3))) {
            count++;
        }
        //Does not include 0.4 as it is not on the board
        assertEquals(24, count);
        Iterator<Chip> iterator =
                instance.iterChipsOnBoard(chips.get(3)).iterator();
        count = 0;
        while (true) {
            try {
                iterator.next();
                count++;
            } catch (NoSuchElementException ex) {
                break;
            }
        }
        //Does not include 0.4 as it is not on the board
        assertEquals(24, count);
    }

    @Test
    public void testGetChipOverLink() {
        Machine instance = new Machine(new MachineDimensions(24, 24),
                new ArrayList<Chip>(), BOOT_CHIP);
        ArrayList<Processor> processors = createProcessors();
        Chip chip =new Chip(new ChipLocation(23, 23), processors,
                ROUTER, SDRAM, null, BOOT_CHIP);
        instance.addChip(chip);
        assertEquals(chip,
                instance.getChipOverLink(chip00, Direction.SOUTHWEST));
    }

    @Test
    public void testNormalizeWithWrapAround() {
        Machine instance = new Machine(new MachineDimensions(48, 24),
                new ArrayList<Chip>(), ChipLocation.ZERO_ZERO);
        assertEquals(new ChipLocation(24, 0), instance.normalizedLocation(24, 24));
        assertEquals(new ChipLocation(24, 1), instance.normalizedLocation(24, 25));
        assertEquals(new ChipLocation(24, 0),
                instance.normalizedLocation(new ChipLocation(24, 24)));
    }

    @Test
    public void testNormalizeWithWrapVertical() {
        Machine instance = new Machine(new MachineDimensions(40, 24),
                new ArrayList<Chip>(), ChipLocation.ZERO_ZERO);
        assertEquals(MachineVersion.TRIAD_WITH_VERTICAL_WRAP, instance.version);
        assertEquals(new ChipLocation(24, 0), instance.normalizedLocation(24, 24));
        assertEquals(new ChipLocation(24, 1), instance.normalizedLocation(24, 25));
    }

    @Test
    public void testNormalizeWithWrapHorizontal() {
        Machine instance = new Machine(new MachineDimensions(48, 16),
                new ArrayList<Chip>(), ChipLocation.ZERO_ZERO);
        assertEquals(MachineVersion.TRIAD_WITH_HORIZONTAL_WRAP, instance.version);
        assertEquals(new ChipLocation(4, 14), instance.normalizedLocation(52, 14));
    }

    @Test
    public void testNormalizeWithOutWrapAround() {
        Machine instance = new Machine(new MachineDimensions(52, 28),
                new ArrayList<Chip>(), ChipLocation.ZERO_ZERO);
        assertEquals(new ChipLocation(24, 24), instance.normalizedLocation(24, 24));
        assertEquals(new ChipLocation(24, 24),
                instance.normalizedLocation(24, 24));
    }

    @Test
    public void testEthernetChip() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = new ArrayList<>();
        byte[] bytes00 = {127, 0, 0, 0};
        Chip chip00 = new Chip(ChipLocation.ZERO_ZERO, processors, null,
                        SDRAM, InetAddress.getByAddress(bytes00), BOOT_CHIP);
        chips.add(chip00);
        byte[] bytes84 = {127, 0, 8, 4};
        Chip chip84 = new Chip(new ChipLocation(8, 4), processors, null,
                        SDRAM, InetAddress.getByAddress(bytes84), BOOT_CHIP);
        chips.add(chip84);
        Chip chip01 = new Chip(new ChipLocation(0, 1), processors, null,
                        SDRAM, null, BOOT_CHIP);
        chips.add(chip01);
        Machine instance = new Machine(
                new MachineDimensions(12, 12), chips, BOOT_CHIP);
        assertEquals(3, instance.nChips());
        assertThat(instance.ethernetConnectedChips(),
                containsInAnyOrder(chip00, chip84));

        byte[] bytes48 = {127, 0, 4, 8};
        Chip chip48 = new Chip(new ChipLocation(4, 8), processors, null,
                        SDRAM, InetAddress.getByAddress(bytes48), BOOT_CHIP);
        instance.addChip(chip48);
        Chip chip02 = new Chip(new ChipLocation(0, 2), processors, null,
                        SDRAM, null, BOOT_CHIP);
        instance.addChip(chip02);
        assertEquals(5, instance.nChips());
        assertThat(instance.ethernetConnectedChips(),
                containsInAnyOrder(chip00, chip84, chip48));
    }

    @Test
    public void testHole() throws UnknownHostException {
        SpiNNakerTriadGeometry geometry =
                SpiNNakerTriadGeometry.getSpinn5Geometry();

        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = new ArrayList<>();
        ArrayList<ChipLocation> all = new ArrayList<>(geometry.singleBoard());
        for (ChipLocation location:all){
            Router router = createRouter(location, all);
            if (location.equals(new ChipLocation(0, 0))) {
                byte[] bytes00 = {127, 0, 0, 0};
                chips.add(new Chip(location, processors, router,
                        SDRAM, InetAddress.getByAddress(bytes00), BOOT_CHIP));
            } else if (location.equals(new ChipLocation(3, 3))) {
                // Leave a hole
            } else {
                 chips.add(new Chip(location, processors, router,
                        SDRAM, null, BOOT_CHIP));
            }
        }

        Machine instance = new Machine(
                new MachineDimensions(12, 12), chips, BOOT_CHIP);
        assertEquals(47, instance.nChips());
        assertEquals("846 cores and 117.0 links",
                instance.coresAndLinkOutputString());

        Set<ChipLocation> abnormalChips = instance.findAbnormalChips();
        assertEquals(0, abnormalChips.size());

        Map<ChipLocation, Set<Direction>> abnormalLinks =
                instance.findAbnormalLinks();
        assertEquals(6, abnormalLinks.size());

        Machine rebuilt = instance.rebuild(abnormalChips, abnormalLinks);
        assertEquals("846 cores and 114.0 links",
                rebuilt.coresAndLinkOutputString());
    }

    @Test
    public void testUnreachable() throws UnknownHostException {
        SpiNNakerTriadGeometry geometry =
                SpiNNakerTriadGeometry.getSpinn5Geometry();

        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = new ArrayList<>();
        ArrayList<ChipLocation> all = new ArrayList<>(geometry.singleBoard());
        for (ChipLocation location:all){
            if (location.equals(new ChipLocation(0, 0))) {
                byte[] bytes00 = {127, 0, 0, 0};
                chips.add(new Chip(location, processors,
                        createRouter(location, all),
                        SDRAM, InetAddress.getByAddress(bytes00), BOOT_CHIP));
            } else if (location.equals(new ChipLocation(3, 3))) {
                 chips.add(new Chip(location, processors, new Router(),
                         SDRAM, null, BOOT_CHIP));
                // Leave a hole
            } else {
                 chips.add(new Chip(location, processors,
                         createRouter(location, all),
                         SDRAM, null, BOOT_CHIP));
            }
        }

        Machine instance = new Machine(
                new MachineDimensions(12, 12), chips, BOOT_CHIP);
        assertEquals(48, instance.nChips());
        assertEquals("864 cores and 117.0 links",
                instance.coresAndLinkOutputString());

        Set<ChipLocation> abnormalChips = instance.findAbnormalChips();
        assertThat(abnormalChips, contains(new ChipLocation(3, 3)));

        Map<ChipLocation, Set<Direction>> abnormalLinks =
                instance.findAbnormalLinks();
        // 6 as it also has only the invers links from 3,3
        assertEquals(6, abnormalLinks.size());

        Machine rebuilt = instance.rebuild(abnormalChips, abnormalLinks);
        assertEquals("846 cores and 114.0 links",
                rebuilt.coresAndLinkOutputString());

        @SuppressWarnings("unused")
        Machine rebuilt2 = rebuilt.rebuild();
     }

    @Test
    public void testUnreachableIncomingChips() {
        Map<ChipLocation, Set<Direction>> ignoreLinks =
                new DefaultMap<>(HashSet::new);
        ignoreLinks.get(new ChipLocation(2, 2)).add(Direction.NORTHEAST);
        ignoreLinks.get(new ChipLocation(2, 3)).add(Direction.EAST);
        ignoreLinks.get(new ChipLocation(3, 4)).add(Direction.SOUTH);
        ignoreLinks.get(new ChipLocation(4, 4)).add(Direction.SOUTHWEST);
        ignoreLinks.get(new ChipLocation(4, 3)).add(Direction.WEST);
        ignoreLinks.get(new ChipLocation(3, 2)).add(Direction.NORTH);

        Machine instance = new VirtualMachine(new MachineDimensions(12, 12),
                null, null, ignoreLinks);
        Map<ChipLocation, Set<Direction>> abnormal =
                instance.findAbnormalLinks();
        assertEquals(1, abnormal.size());
    }

    @Test
    public void testEquals() throws UnknownHostException {
        ArrayList<Processor> processors = createProcessors();
        ArrayList<Chip> chips = createdChips(processors);
        /* InetAddress address =*/ InetAddress.getByAddress(bytes);

        Machine instance1 = new Machine(
                new MachineDimensions(8, 8), chips, BOOT_CHIP);

        Machine instance2 = new Machine(
                new MachineDimensions(8, 8), chips, BOOT_CHIP);

        assertEquals(instance1, instance2);

        chips.remove(3);

        Machine missingChip = new Machine(
                new MachineDimensions(8, 8), chips, BOOT_CHIP);

        assertNotEquals(instance1, missingChip);
        assertNotNull(instance1.difference(missingChip));
   }

}
