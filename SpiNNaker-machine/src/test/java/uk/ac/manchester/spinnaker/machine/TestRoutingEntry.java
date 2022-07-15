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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Christian-B
 */
public class TestRoutingEntry {

	public TestRoutingEntry() {
	}

	@Test
	public void testBasic() {
		var directions = asList(Direction.NORTH, Direction.SOUTH);
		var ids = asList(4, 6, 8);
		var instance = new RoutingEntry(ids, directions);

		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertThat(decode.getLinkIDs(), contains(directions.toArray()));
		assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
	}

	@Test
	public void testSingleId() {
		var directions = asList(Direction.NORTH, Direction.SOUTH);
		var ids = asList(4);
		var instance = new RoutingEntry(ids, directions);
		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertThat(decode.getLinkIDs(), contains(directions.toArray()));
		assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
	}

	@Test
	public void testEmptyId() {
		var directions = asList(Direction.NORTH, Direction.SOUTH);
		var instance = new RoutingEntry(emptyList(), directions);
		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertThat(decode.getLinkIDs(), contains(directions.toArray()));
		assertEquals(0, decode.getProcessorIDs().size());
	}

	@Test
	public void testNegative() {
		var directions = asList(Direction.NORTH, Direction.SOUTH);
		var ids = asList(4, -66, 8);
		assertThrows(Exception.class, () -> {
			@SuppressWarnings("unused")
			var instance = new RoutingEntry(ids, directions);
		});
	}

	@Test
	public void testTooHighId() {
		var directions = asList(Direction.NORTH, Direction.SOUTH);
		var ids = asList(4, 60, 8);
		assertThrows(Exception.class, () -> {
			@SuppressWarnings("unused")
			var instance = new RoutingEntry(ids, directions);
		});
	}

	@Test
	public void testOneDirection() {
		var directions = asList(Direction.SOUTH);
		var ids = asList(4, 6, 8);
		var instance = new RoutingEntry(ids, directions);
		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertThat(decode.getLinkIDs(), contains(Direction.SOUTH));
		assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
	}

	@Test
	public void testEmptyDirection() {
		var ids = asList(4, 6, 8);
		var instance = new RoutingEntry(ids, emptySet());
		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertEquals(0, decode.getLinkIDs().size());
		assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
	}

	@Test
	public void testDoubleEmpty() {
		var instance = new RoutingEntry(emptySet(), emptyList());
		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertEquals(0, decode.getProcessorIDs().size());
		assertEquals(0, decode.getLinkIDs().size());
	}

	@Test
	public void testDouble() {
		var directions = asList(Direction.NORTH, Direction.SOUTH);
		var ids = asList(4, 6, 8);
		var directions2 =
				asList(Direction.NORTH, Direction.SOUTH, Direction.SOUTH);
		var ids2 = asList(4, 6, 8, 4);
		var instance = new RoutingEntry(ids2, directions2);

		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertThat(decode.getLinkIDs(), contains(directions.toArray()));
		assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
	}

	@Test
	public void testUnordered() {
		var directions = asList(Direction.NORTH, Direction.SOUTH);
		var ids = asList(4, 6, 8);
		var directions2 = asList(Direction.SOUTH, Direction.NORTH);
		var ids2 = asList(6, 4, 8);
		var instance = new RoutingEntry(ids2, directions2);

		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertThat(decode.getLinkIDs(), contains(directions.toArray()));
		assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
	}
}
