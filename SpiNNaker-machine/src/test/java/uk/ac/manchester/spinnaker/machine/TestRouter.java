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

import java.net.UnknownHostException;
import java.util.ArrayList;
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
        var links = new ArrayList<Link>();
        links.add(link00_01);
        @SuppressWarnings("unused")
        var router = new Router(links);
    }

    @Test
    public void testLinks() {
        var links = new ArrayList<Link>();
        links.add(link00_01);
        var router = new Router(links);
        var values = router.links();
        assertEquals(1, values.size());
        assertThrows(UnsupportedOperationException.class, () -> {
            values.remove(link00_01);
        });
        var values2 = router.links();
        assertEquals(1, values2.size());
    }

    @Test
    public void testgetNeighbouringChipsCoords() throws UnknownHostException {
        var links = new ArrayList<Link>();
        links.add(link00_10);
        links.add(link00_01);
        assertThat(chip01, is(oneOf(chip01, chip10)));
        var router = new Router(links);
        var neighbours = router.streamNeighbouringChipsCoords();
        neighbours.forEach(loc -> {
        	assertThat(loc, is(oneOf(chip01, chip10)));
        });
        //Streams can only be run through ONCE!
        assertThrows(IllegalStateException.class, () -> {
            neighbours.forEach(loc -> {
                assertThat(loc, is(oneOf(chip01, chip10)));
            });
        });
        for (var loc: router.iterNeighbouringChipsCoords()) {
        	assertThat(loc, is(oneOf(chip01, chip10)));
        }
        var iterator = router.iterNeighbouringChipsCoords().iterator();
        // Note Order is now by Direction
        assertEquals(chip01, iterator.next());
        assertEquals(chip10, iterator.next());
        assertFalse(iterator.hasNext());
        assertThat(router.neighbouringChipsCoords(),
               containsInAnyOrder(chip01, chip10));
    }

    @Test
    public void testRouterStream() {
        var links = new ArrayList<Link>();
        links.add(link00_01);
        links.add(link01_01);
        var router = new Router(links.stream());
        assertTrue(router.hasLink(Direction.NORTH));
        assertEquals(link00_01, router.getLink(Direction.NORTH));
        assertEquals(2, router.size());
    }

    @Test
    public void testRouterRepeat() {
        var links = new ArrayList<Link>();
        links.add(link00_01);
        links.add(link00_01a);
        assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("unused")
            var router = new Router(links);
        });
    }

    @Test
    public void testDefaults1() {
        var router = new Router();
        assertThat(router.links(), IsEmptyCollection.empty());
        assertEquals(MachineDefaults.ROUTER_AVAILABLE_ENTRIES, router.nAvailableMulticastEntries);
    }

    @Test
    public void testDefaults2() {
        var links = new ArrayList<Link>();
        links.add(link00_01);
        links.add(link00_10);
        var router = new Router(links);
        assertThat(router.links(), containsInAnyOrder(links.toArray()));
        assertEquals(MachineDefaults.ROUTER_AVAILABLE_ENTRIES, router.nAvailableMulticastEntries);
    }

    @Test
    public void testDefaults3() {
        var links = new ArrayList<Link>();
        links.add(link00_01);
        links.add(link00_10);
        var router = new Router(
                links, MachineDefaults.ROUTER_AVAILABLE_ENTRIES + 1);
        assertThat(router.links(), containsInAnyOrder(links.toArray()));
        assertEquals(MachineDefaults.ROUTER_AVAILABLE_ENTRIES + 1,
                router.nAvailableMulticastEntries);
    }

}
