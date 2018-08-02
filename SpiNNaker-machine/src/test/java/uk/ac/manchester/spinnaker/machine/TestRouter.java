/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;


/**
 *
 * @author Christian-B
 * @deprecated Merged into Chip left mainly as an Example for Streams
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
        links.add(link00_01);
        links.add(link01_01);
        Router router = new Router(links);
        Stream<HasChipLocation> neighbours = router.streamNeighbouringChipsCoords();
        neighbours.forEach(loc -> {
                assertEquals(loc, chip01);
            });
        assertThrows(IllegalStateException.class, () -> {
            neighbours.forEach(loc -> {
                assertEquals(loc, chip01);
            });
        });
        for (HasChipLocation loc:router.iterNeighbouringChipsCoords()){
            assertEquals(loc, chip01);
        }
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




}
