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
package uk.ac.manchester.spinnaker.machine.datalinks;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTHEAST;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 *
 * @author Christian-B
 */
public class TestAbstractDataLink {
	private static InetAddress localhost() throws UnknownHostException {
		byte[] bytes = {127, 0, 0, 1};
		return InetAddress.getByAddress(bytes);
	}

	@Test
	public void testEquals() throws UnknownHostException {
		var link1 = new ADL(ZERO_ZERO, NORTHEAST, localhost());
		var link2 = new ADL(ZERO_ZERO, NORTHEAST, localhost());
		assertEquals(link1, link2);
		assertEquals(link1, link1);
		assertNotEquals(link1, null);
		assertNotEquals(link1, "link1");
	}

	/** Can't use abstract class instances directly. */
	static class ADL extends AbstractDataLink {
		ADL(HasChipLocation location, Direction linkId,
				InetAddress boardAddress) {
			super(location, linkId, boardAddress);
		}

		@Override
		public int hashCode() {
			return Objects.hash(location, direction);
		}

		@Override
		public boolean equals(Object obj) {
			return (obj instanceof AbstractDataLink link) && sameAs(link);
		}
	}
}
