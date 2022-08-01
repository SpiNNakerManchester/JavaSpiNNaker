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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian-B
 */
public class TestRouter {

	private static final ChipLocation CHIP00 = new ChipLocation(0, 0);

	private static final ChipLocation CHIP01 = new ChipLocation(0, 1);

	private static final ChipLocation CHIP10 = new ChipLocation(1, 0);

	private static final Link LINK00_01 =
			new Link(CHIP00, Direction.NORTH, CHIP01);

	private static final Link LINK00_01A =
			new Link(CHIP00, Direction.NORTH, CHIP01);

	private static final Link LINK00_10 =
			new Link(CHIP00, Direction.WEST, CHIP10);

	private static final Link LINK01_01 =
			new Link(CHIP01, Direction.SOUTH, CHIP01);

	@Test
	public void testRouterBasicUse() {
		ArrayList<Link> links = new ArrayList<>();
		links.add(LINK00_01);
		@SuppressWarnings("unused")
		Router router = new Router(links);
	}

	@Test
	public void testLinks() {
		ArrayList<Link> links = new ArrayList<>();
		links.add(LINK00_01);
		Router router = new Router(links);
		final Collection<Link> values = router.links();
		assertEquals(1, values.size());
		assertThrows(UnsupportedOperationException.class, () -> {
			values.remove(LINK00_01);
		});
		Collection<Link> values2 = router.links();
		assertEquals(1, values2.size());
	}

	@Test
	public void testgetNeighbouringChipsCoords() {
		ArrayList<Link> links = new ArrayList<>();
		links.add(LINK00_10);
		links.add(LINK00_01);
		assertThat(CHIP01, is(oneOf(CHIP01, CHIP10)));
		Router router = new Router(links);
		Stream<ChipLocation> neighbours =
				router.streamNeighbouringChipsCoords();
		neighbours.forEach(loc -> {
			assertThat(loc, is(oneOf(CHIP01, CHIP10)));
		});
		// Streams can only be run through ONCE!
		assertThrows(IllegalStateException.class, () -> {
			neighbours.forEach(loc -> {
				assertThat(loc, is(oneOf(CHIP01, CHIP10)));
			});
		});
		for (HasChipLocation loc : router.iterNeighbouringChipsCoords()) {
			assertThat(loc, is(oneOf(CHIP01, CHIP10)));
		}
		Iterator<ChipLocation> iterator =
				router.iterNeighbouringChipsCoords().iterator();
		// Note Order is now by Direction
		assertEquals(CHIP01, iterator.next());
		assertEquals(CHIP10, iterator.next());
		assertFalse(iterator.hasNext());
		assertThat(router.neighbouringChipsCoords(),
				containsInAnyOrder(CHIP01, CHIP10));
	}

	@Test
	public void testRouterStream() {
		ArrayList<Link> links = new ArrayList<>();
		links.add(LINK00_01);
		links.add(LINK01_01);
		Router router = new Router(links.stream());
		assertTrue(router.hasLink(Direction.NORTH));
		assertEquals(LINK00_01, router.getLink(Direction.NORTH));
		assertEquals(2, router.size());
	}

	@Test
	public void testRouterRepeat() {
		ArrayList<Link> links = new ArrayList<>();
		links.add(LINK00_01);
		links.add(LINK00_01A);
		assertThrows(IllegalArgumentException.class, () -> {
			@SuppressWarnings("unused")
			Router router = new Router(links);
		});
	}

	@Test
	public void testDefaults1() {
		Router router = new Router();
		assertThat(router.links(), IsEmptyCollection.empty());
		assertEquals(MachineDefaults.ROUTER_AVAILABLE_ENTRIES,
				router.nAvailableMulticastEntries);
	}

	@Test
	public void testDefaults2() {
		ArrayList<Link> links = new ArrayList<>();
		links.add(LINK00_01);
		links.add(LINK00_10);
		Router router = new Router(links);
		assertThat(router.links(), containsInAnyOrder(links.toArray()));
		assertEquals(MachineDefaults.ROUTER_AVAILABLE_ENTRIES,
				router.nAvailableMulticastEntries);
	}

	@Test
	public void testDefaults3() {
		ArrayList<Link> links = new ArrayList<>();
		links.add(LINK00_01);
		links.add(LINK00_10);
		Router router =
				new Router(links, MachineDefaults.ROUTER_AVAILABLE_ENTRIES + 1);
		assertThat(router.links(), containsInAnyOrder(links.toArray()));
		assertEquals(MachineDefaults.ROUTER_AVAILABLE_ENTRIES + 1,
				router.nAvailableMulticastEntries);
	}

}
