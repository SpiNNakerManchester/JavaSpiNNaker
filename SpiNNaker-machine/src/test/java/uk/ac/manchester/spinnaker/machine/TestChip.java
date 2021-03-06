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
import java.util.Iterator;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 *
 * @author Christian-B
 */
public class TestChip {

    ChipLocation location00 = new ChipLocation(0,0);
    ChipLocation location01 = new ChipLocation(0,1);
    ChipLocation location10 = new ChipLocation(1,0);
    ChipLocation location11 = new ChipLocation(1,1);

    Link link00_01 = new Link(location00, Direction.NORTH, location01);
    Link link00_01a = new Link(location00, Direction.NORTH, location01);
    Link link00_10 = new Link(location00, Direction.WEST, location10);
    //Link link01_01 = new Link(location01, Direction.SOUTH, location01);


    private Router createRouter() {
        ArrayList<Link> links = new ArrayList<>();
        links.add(link00_10);
        links.add(link00_01);
        return new Router(links);
    }

    @SuppressWarnings("unused")
    private ArrayList<Link> createLinks() {
        ArrayList<Link> links = new ArrayList<>();
        links.add(link00_10);
        links.add(link00_01);
        return links;
    }

    private InetAddress createInetAddress() throws UnknownHostException {
        byte[] bytes = {127,0,0,0};
        return InetAddress.getByAddress(bytes);
    }

    private ArrayList<Processor> getProcessors() {
        ArrayList<Processor> processors = new ArrayList<>();
        processors.add(Processor.factory(1));
        processors.add(Processor.factory(2, true));
        processors.add(Processor.factory(4));
        return processors;
    }

    @Test
    public void testChipBasic() throws UnknownHostException {
        ArrayList<Integer> tags =
                new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
        Chip chip = new Chip(location00, getProcessors(), createRouter(), 100,
                createInetAddress(), false,  tags, location11);
        assertEquals(0, chip.getX());
        assertEquals(0, chip.getY());
        assertEquals(3, chip.nProcessors());
        assertEquals(2, chip.nUserProcessors());
        assertFalse(chip.hasUserProcessor(2));
        assertTrue(chip.hasUserProcessor(4));
        assertFalse(chip.hasUserProcessor(3));
        assertNull(chip.getUserProcessor(2));
        assertEquals(Processor.factory(4), chip.getUserProcessor(4));
        assertNull(chip.getUserProcessor(3));
        //contains check that is has exactly these elements in order
        assertThat(chip.userProcessors(), contains(
                Processor.factory(1),
                Processor.factory(4)));
    }

    @Test
    public void testDefault() throws UnknownHostException {
        ArrayList<Integer> tags =
                new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        Chip chip = new Chip(location00, getProcessors(), createRouter(),
                100, createInetAddress(), location11);
        assertEquals(tags, chip.getTagIds());
        assertFalse(chip.virtual);
    }

    @Test
    public void testChipMonitors() throws UnknownHostException {
        ArrayList<Integer> tags =
                new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
        Chip chip = new Chip(location00, getProcessors(), createRouter(), 100,
                createInetAddress(), false, tags, location11);
        Processor result = chip.getFirstUserProcessor();
        assertEquals(Processor.factory(1), result);
        assertEquals(2, chip.nUserProcessors());
    }

    /**
     * Test of toString method, of class Chip.
     */
    @Test
    public void testToString() throws UnknownHostException {
        ArrayList<Integer> tags =
                new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
        Chip chip1 = new Chip(location00, getProcessors(), createRouter(), 100,
                createInetAddress(), false, tags, location11);
        Chip chip2 = new Chip(location00, getProcessors(), createRouter(), 100,
                createInetAddress(), false, tags, location11);
        assertEquals(chip1.toString(), chip2.toString());
    }

    @Test
    public void testRepeatMonitor() throws UnknownHostException {
        ArrayList<Processor> processors = getProcessors();
        processors.add(Processor.factory(2, false));
        assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("unused")
            Chip chip = new Chip(
                    ChipLocation.ZERO_ZERO, processors, createRouter(), 100,
                    createInetAddress(), location11);
        });
    }

    @Test
    public void testRepeatUser() throws UnknownHostException {
        ArrayList<Processor> processors = getProcessors();
        processors.add(Processor.factory(4, true));
        assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("unused")
            Chip chip = new Chip(
                    ChipLocation.ZERO_ZERO, processors, createRouter(), 100,
                    createInetAddress(), location11);
        });
    }

    @Test
    public void testAsLocation() throws UnknownHostException {
        Chip chip1 = new Chip(ChipLocation.ZERO_ZERO, getProcessors(),
                createRouter(), 100, createInetAddress(), false, null, location11);
        assertEquals(ChipLocation.ZERO_ZERO, chip1.asChipLocation());
    }

    @Test
    public void testGet() throws UnknownHostException {
        Chip chip1 = new Chip(new ChipLocation(3, 4), getProcessors(),
                createRouter(), 100, createInetAddress(), location11);
        assertEquals(3, chip1.getX());
        assertEquals(4, chip1.getY());
        assertEquals(new ChipLocation(3,4), chip1.asChipLocation());
    }

    @Test
    public void testDefault1() throws UnknownHostException {
        Chip chip = new Chip(ChipLocation.ONE_ZERO, createRouter(),
                null, location11);
        assertEquals(ChipLocation.ONE_ZERO, chip.asChipLocation());
        assertFalse(chip.virtual, "Chips on virtual nmachine are not virtual!");
        assertEquals(17, chip.nUserProcessors());
        assertEquals(18, chip.nProcessors());
        assertEquals(MachineDefaults.SDRAM_PER_CHIP, chip.sdram);
        assert(chip.getTagIds().isEmpty());
   }

    @Test
    public void testDefault2() throws UnknownHostException {
        ArrayList<Integer> tags =
                new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        Chip chip = new Chip(ChipLocation.ZERO_ZERO, createRouter(),
                createInetAddress(), location11);
        assertEquals(ChipLocation.ZERO_ZERO, chip.asChipLocation());
        assertFalse(chip.virtual, "Chips on virtual nmachine are not virtual!");
        assertEquals(17, chip.nUserProcessors());
        assertEquals(18, chip.nProcessors());
        assertEquals(MachineDefaults.SDRAM_PER_CHIP, chip.sdram);
        assertEquals(tags, chip.getTagIds());
    }

    @Test
    public void testLinksVirtualMachine() throws UnknownHostException {
        Chip chip = new Chip(ChipLocation.ZERO_ZERO, createRouter(),
                createInetAddress(), location11);
        final Collection<Link> values = chip.router.links();
        assertEquals(2, values.size());
        assertThrows(UnsupportedOperationException.class, () -> {
            values.remove(link00_01);
        });
        Collection<Link> values2 = chip.router.links();
        assertEquals(2, values2.size());
        Iterator<Link> iterator = values2.iterator();
        assertEquals(link00_01, iterator.next());
        assertEquals(link00_10, iterator.next());
        assertFalse(iterator.hasNext());
        assert(chip.router.hasLink(Direction.NORTH));
    }

}
