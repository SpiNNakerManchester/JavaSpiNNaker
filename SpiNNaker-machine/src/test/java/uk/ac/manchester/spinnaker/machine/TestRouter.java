/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.jupiter.api.*;


/**
 *
 * @author Christian-B
 */
public class TestRouter {

    ChipLocation chip00 = new ChipLocation(0,0);
    ChipLocation chip01 = new ChipLocation(0,1);
    ChipLocation chip10 = new ChipLocation(1,0);
    ChipLocation chip11 = new ChipLocation(1,1);

    Link link00_01 = new Link(chip00, Direction.NORTH, chip01);
    Link link00_01a = new Link(chip00, Direction.NORTH, chip01);
    Link link00_10 = new Link(chip00, Direction.WEST, chip10);
    Link link01_01 = new Link(chip01, Direction.SOUTH, chip01);

    @Test
    public void testRouterBasicUse() {
        ArrayList<Link> links = new ArrayList<>();
        links.add(link00_01);
        Router router = new Router(links);
    }

    @Test
    public void testLinks() {
        ArrayList<Link> links = new ArrayList<>();
        links.add(link00_01);
        Router router = new Router(links);
        final Collection<Link> values = router.links();
        assertEquals(1, values.size());
        assertThrows(UnsupportedOperationException.class, () -> {
            values.remove(link00_01);
        });
        Collection<Link> values2 = router.links();
        assertEquals(1, values2.size());
    }

    @Test
    public void testgetNeighbouringChipsCoords() throws UnknownHostException {
        ArrayList<Link> links = new ArrayList<>();
        links.add(link00_10);
        links.add(link00_01);
        assertThat(chip01, is(oneOf(chip01, chip10)));
        Router router = new Router(links);
        Stream<ChipLocation> neighbours = router.streamNeighbouringChipsCoords();
        neighbours.forEach(loc -> {
                assertThat(loc, is(oneOf(chip01, chip10)));
            });
        //Streams can only be run through ONCE!
        assertThrows(IllegalStateException.class, () -> {
            neighbours.forEach(loc -> {
                assertThat(loc, is(oneOf(chip01, chip10)));
            });
        });
        for (HasChipLocation loc:router.iterNeighbouringChipsCoords()){
                assertThat(loc, is(oneOf(chip01, chip10)));
        }
        Iterator<ChipLocation> iterator =
                router.iterNeighbouringChipsCoords().iterator();
        // Note Order is now by Direction
        assertEquals(chip01, iterator.next());
        assertEquals(chip10, iterator.next());
        assertFalse(iterator.hasNext());
        assertThat(router.neighbouringChipsCoords(),
               containsInAnyOrder(chip01, chip10));
    }

    @Test
    public void testRouterStream() {
        ArrayList<Link> links = new ArrayList<>();
        links.add(link00_01);
        links.add(link01_01);
        Router router = new Router(links.stream());
        assertTrue(router.hasLink(Direction.NORTH));
        assertEquals(link00_01, router.getLink(Direction.NORTH));
        assertEquals(2, router.size());
    }

    @Test
    public void testRouterRepeat() {
        ArrayList<Link> links = new ArrayList<>();
        links.add(link00_01);
        links.add(link00_01a);
        assertThrows(IllegalArgumentException.class, () -> {
            Router router = new Router(links);
        });
    }

    @Test
    public void testDefaults1() {
        Router router = new Router();
        assertThat(router.links(), IsEmptyCollection.empty());
        assertEquals(MachineDefaults.ROUTER_CLOCK_SPEED, router.clockSpeed);
        assertEquals(MachineDefaults.ROUTER_AVAILABLE_ENTRIES, router.nAvailableMulticastEntries);
    }

    @Test
    public void testDefaults2() {
        ArrayList<Link> links = new ArrayList<>();
        links.add(link00_01);
        links.add(link00_10);
        Router router = new Router(links);
        assertThat(router.links(), containsInAnyOrder(links.toArray()));
        assertEquals(MachineDefaults.ROUTER_CLOCK_SPEED, router.clockSpeed);
        assertEquals(MachineDefaults.ROUTER_AVAILABLE_ENTRIES, router.nAvailableMulticastEntries);
    }

    @Test
    public void testDefaults3() {
        ArrayList<Link> links = new ArrayList<>();
        links.add(link00_01);
        links.add(link00_10);
        Router router = new Router(
                links, MachineDefaults.ROUTER_AVAILABLE_ENTRIES + 1);
        assertThat(router.links(), containsInAnyOrder(links.toArray()));
        assertEquals(MachineDefaults.ROUTER_CLOCK_SPEED, router.clockSpeed);
        assertEquals(MachineDefaults.ROUTER_AVAILABLE_ENTRIES + 1,
                router.nAvailableMulticastEntries);
    }

}
