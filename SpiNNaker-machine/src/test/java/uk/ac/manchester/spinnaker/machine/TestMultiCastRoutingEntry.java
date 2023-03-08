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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

/**
 *
 * @author Christian-B
 */
public class TestMultiCastRoutingEntry {

	public TestMultiCastRoutingEntry() {
	}

	@Test
	public void testBasic() {
		var directions = List.of(Direction.NORTH, Direction.SOUTH);
		var ids = List.of(4, 6, 8);
		int key = 100;
		int mask = 200;
		var instance =
				new MulticastRoutingEntry(key, mask, ids, directions, true);

		assertEquals(key, instance.getKey());
		assertEquals(mask, instance.getMask());
		assertTrue(instance.isDefaultable());

		var decode =
				new MulticastRoutingEntry(key, mask, instance.encode(), true);

		assertThat(decode.getLinkIDs(), contains(directions.toArray()));
		assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
	}

}
