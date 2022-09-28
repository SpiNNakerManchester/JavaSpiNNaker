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
package uk.ac.manchester.spinnaker.machine.datalinks;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTHEAST;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 *
 * @author Christian-B
 */
public class TestAbstractDataLink {
	private static InetAddress localhost() throws UnknownHostException {
		byte[] bytes = {127, 0, 0, 0};
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
			return hash();
		}

		@Override
		public boolean equals(Object obj) {
			return (obj instanceof AbstractDataLink)
					&& sameAs((AbstractDataLink) obj);
		}
	}
}
