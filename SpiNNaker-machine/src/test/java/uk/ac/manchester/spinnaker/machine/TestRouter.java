/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTH;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTH;
import static uk.ac.manchester.spinnaker.machine.Direction.WEST;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.ROUTER_AVAILABLE_ENTRIES;
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

	private static final Link LINK00_01 = new Link(CHIP00, NORTH, CHIP01);

	private static final Link LINK00_01A = new Link(CHIP00, NORTH, CHIP01);

	private static final Link LINK00_10 = new Link(CHIP00, WEST, CHIP10);

	private static final Link LINK01_01 = new Link(CHIP01, SOUTH, CHIP01);

	@Test
	public void testRouterBasicUse() {
		var links = List.of(LINK00_01);
		@SuppressWarnings("unused")
		var router = new Router(links);
	}

	@Test
	public void testLinks() {
		var links = List.of(LINK00_01);
		var router = new Router(links);
		final var values = router.links();
		assertEquals(1, values.size());
		assertThrows(UnsupportedOperationException.class, () -> {
			values.remove(LINK00_01);
		});
		var values2 = router.links();
		assertEquals(1, values2.size());
	}

	@Test
	public void testgetNeighbouringChipsCoords() {
		var links = List.of(LINK00_10, LINK00_01);
		assertThat(CHIP01, is(oneOf(CHIP01, CHIP10)));
		var router = new Router(links);
		var neighbours = router.streamNeighbouringChipsCoords();
		neighbours.forEach(loc -> {
			assertThat(loc, is(oneOf(CHIP01, CHIP10)));
		});
		// Streams can only be run through ONCE!
		assertThrows(IllegalStateException.class, () -> {
			neighbours.forEach(loc -> {
				assertThat(loc, is(oneOf(CHIP01, CHIP10)));
			});
		});
		for (var loc : router.iterNeighbouringChipsCoords()) {
			assertThat(loc, is(oneOf(CHIP01, CHIP10)));
		}
		var iterator = router.iterNeighbouringChipsCoords().iterator();
		// Note Order is now by Direction
		assertEquals(CHIP01, iterator.next());
		assertEquals(CHIP10, iterator.next());
		assertFalse(iterator.hasNext());
		assertThat(router.neighbouringChipsCoords(),
				containsInAnyOrder(CHIP01, CHIP10));
	}

	@Test
	public void testRouterStream() {
		var links = List.of(LINK00_01, LINK01_01);
		var router = new Router(links.stream());
		assertTrue(router.hasLink(Direction.NORTH));
		assertEquals(LINK00_01, router.getLink(Direction.NORTH));
		assertEquals(2, router.size());
	}

	@Test
	public void testRouterRepeat() {
		var links = List.of(LINK00_01, LINK00_01A);
		assertThrows(IllegalArgumentException.class, () -> {
			@SuppressWarnings("unused")
			var router = new Router(links);
		});
	}

	@Test
	public void testDefaults1() {
		var router = new Router();
		assertThat(router.links(), IsEmptyCollection.empty());
		assertEquals(ROUTER_AVAILABLE_ENTRIES,
				router.nAvailableMulticastEntries);
	}

	@Test
	public void testDefaults2() {
		var links = List.of(LINK00_01, LINK00_10);
		var router = new Router(links);
		assertThat(router.links(), containsInAnyOrder(links.toArray()));
		assertEquals(ROUTER_AVAILABLE_ENTRIES,
				router.nAvailableMulticastEntries);
	}

	@Test
	public void testDefaults3() {
		var links = List.of(LINK00_01, LINK00_10);
		var router =
				new Router(links, MachineDefaults.ROUTER_AVAILABLE_ENTRIES + 1);
		assertThat(router.links(), containsInAnyOrder(links.toArray()));
		assertEquals(ROUTER_AVAILABLE_ENTRIES + 1,
				router.nAvailableMulticastEntries);
	}

}
