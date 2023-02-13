/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.machine;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTH;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTH;

import java.util.List;
import java.util.Set;

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
		var directions = List.of(NORTH, SOUTH);
		var ids = List.of(4, 6, 8);
		var instance = new RoutingEntry(ids, directions);

		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertThat(decode.getLinkIDs(), contains(directions.toArray()));
		assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
	}

	@Test
	public void testSingleId() {
		var directions = List.of(NORTH, SOUTH);
		var ids = List.of(4);
		var instance = new RoutingEntry(ids, directions);
		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertThat(decode.getLinkIDs(), contains(directions.toArray()));
		assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
	}

	@Test
	public void testEmptyId() {
		var directions = List.of(NORTH, SOUTH);
		var instance = new RoutingEntry(List.of(), directions);
		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertThat(decode.getLinkIDs(), contains(directions.toArray()));
		assertEquals(0, decode.getProcessorIDs().size());
	}

	@Test
	public void testNegative() {
		var directions = List.of(NORTH, SOUTH);
		var ids = List.of(4, -66, 8);
		assertThrows(Exception.class, () -> {
			@SuppressWarnings("unused")
			var instance = new RoutingEntry(ids, directions);
		});
	}

	@Test
	public void testTooHighId() {
		var directions = List.of(NORTH, SOUTH);
		var ids = List.of(4, 60, 8);
		assertThrows(Exception.class, () -> {
			@SuppressWarnings("unused")
			var instance = new RoutingEntry(ids, directions);
		});
	}

	@Test
	public void testOneDirection() {
		var directions = List.of(SOUTH);
		var ids = List.of(4, 6, 8);
		var instance = new RoutingEntry(ids, directions);
		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertThat(decode.getLinkIDs(), contains(SOUTH));
		assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
	}

	@Test
	public void testEmptyDirection() {
		var ids = List.of(4, 6, 8);
		var instance = new RoutingEntry(ids, Set.of());
		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertEquals(0, decode.getLinkIDs().size());
		assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
	}

	@Test
	public void testDoubleEmpty() {
		var instance = new RoutingEntry(Set.of(), List.of());
		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertEquals(0, decode.getProcessorIDs().size());
		assertEquals(0, decode.getLinkIDs().size());
	}

	@Test
	public void testDouble() {
		var directions = List.of(NORTH, SOUTH);
		var ids = List.of(4, 6, 8);
		var directions2 = List.of(NORTH, SOUTH, SOUTH);
		var ids2 = List.of(4, 6, 8, 4);
		var instance = new RoutingEntry(ids2, directions2);

		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertThat(decode.getLinkIDs(), contains(directions.toArray()));
		assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
	}

	@Test
	public void testUnordered() {
		var directions = List.of(NORTH, SOUTH);
		var ids = List.of(4, 6, 8);
		var directions2 = List.of(SOUTH, NORTH);
		var ids2 = List.of(6, 4, 8);
		var instance = new RoutingEntry(ids2, directions2);

		int code = instance.encode();

		var decode = new RoutingEntry(code);
		assertThat(decode.getLinkIDs(), contains(directions.toArray()));
		assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
	}
}
