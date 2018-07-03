/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
    //Link link00_01a = new Link(location00, Direction.NORTH, location01);
    Link link00_10 = new Link(location00, Direction.WEST, location10);
    //Link link01_01 = new Link(location01, Direction.SOUTH, location01);


    private Router createRouter() {
        ArrayList<Link> links = new ArrayList<>();
        links.add(link00_01);
        links.add(link00_10);
        return new Router(links);
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
        Chip chip = new Chip(0, 0, getProcessors(), createRouter(), 100,
                createInetAddress(), false, 6, location11);
        assertEquals(0, chip.getX());
        assertEquals(0, chip.getY());
        assertTrue(chip.hasProcessor(4));
        assertFalse(chip.hasProcessor(3));
        assertEquals(Processor.factory(4), chip.getProcessor(4));
        assertNull(chip.getProcessor(3));
        assertThat(chip.processors(), contains(
                Processor.factory(1), Processor.factory(2, true),
                Processor.factory(4)));
        assertEquals(3, chip.nProcessors());
    }

    @Test
    public void testChipMonitors() throws UnknownHostException {
        Chip chip = new Chip(0, 0, getProcessors(), createRouter(), 100,
                createInetAddress(), false, 6, location11);
        Processor result = chip.getFirstNoneMonitorProcessor();
        assertEquals(Processor.factory(1), result);
        assertEquals(2, chip.nUserProcessors());
        assertEquals(1, chip.reserveASystemProcessor());

        result = chip.getFirstNoneMonitorProcessor();
        assertEquals(Processor.factory(4), result);
        assertEquals(1, chip.nUserProcessors());
        assertEquals(Processor.factory(4), result);

        assertEquals(4, chip.reserveASystemProcessor());
        assertEquals(0, chip.nUserProcessors());
        assertThrows(IllegalStateException.class, () -> {
            Processor bad = chip.getFirstNoneMonitorProcessor();
        });
        assertThrows(IllegalStateException.class, () -> {
            int bad = chip.reserveASystemProcessor();
        });
    }

    /**
     * Test of toString method, of class Chip.
     */
    @Test
    public void testToString() throws UnknownHostException {
        Chip chip1 = new Chip(0, 0, getProcessors(), createRouter(), 100,
                createInetAddress(), false, 6, location11);
        Chip chip2 = new Chip(0, 0, getProcessors(), createRouter(), 100,
                createInetAddress(), false, 6, location11);
        assertNotEquals(chip1.toString(), chip2.toString());
    }

    @Test
    public void testRepeat() throws UnknownHostException {
        ArrayList<Processor> processors = getProcessors();
        processors.add(Processor.factory(2, false));
        assertThrows(IllegalArgumentException.class, () -> {
            Chip chip = new Chip(0, 0, processors, createRouter(), 100,
                    createInetAddress(), false, 6, location11);
        });
    }


}
