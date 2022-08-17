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

import java.util.Arrays;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Christian-B
 */
public class TestMultiCastRoutingEntry {

	public TestMultiCastRoutingEntry() {
	}

	@Test
	public void testBasic() {
		List<Direction> directions =
				Arrays.asList(Direction.NORTH, Direction.SOUTH);
		List<Integer> ids = Arrays.asList(4, 6, 8);
		int key = 100;
		int mask = 200;
		MulticastRoutingEntry instance =
				new MulticastRoutingEntry(key, mask, ids, directions, true);

		assertEquals(key, instance.getKey());
		assertEquals(mask, instance.getMask());
		assertTrue(instance.isDefaultable());

		MulticastRoutingEntry decode =
				new MulticastRoutingEntry(key, mask, instance.encode(), true);

		assertThat(decode.getLinkIDs(), contains(directions.toArray()));
		assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
	}

}
