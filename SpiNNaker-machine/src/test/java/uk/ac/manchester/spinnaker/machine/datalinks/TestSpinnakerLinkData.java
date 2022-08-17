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

import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;

/**
 *
 * @author Christian-B
 */
public class TestSpinnakerLinkData {

	private ChipLocation location00 = new ChipLocation(0, 0);

	private ChipLocation location01 = new ChipLocation(0, 1);

	private InetAddress createInetAddress() throws UnknownHostException {
		byte[] bytes = {127, 0, 0, 0};
		return InetAddress.getByAddress(bytes);
	}

	public TestSpinnakerLinkData() {
	}

	private void checkDifferent(SpinnakerLinkData link1,
			SpinnakerLinkData link2) {
		assertNotEquals(link1, link2);
		assertNotEquals(link1.hashCode(), link2.hashCode());
		assertNotEquals(link1.toString(), link2.toString());
	}

	private void checkSame(SpinnakerLinkData link1, SpinnakerLinkData link2) {
		assertEquals(link1, link2);
		assertEquals(link1.hashCode(), link2.hashCode());
		assertEquals(link1.toString(), link2.toString());
	}

	@Test
	public void testEquals() throws UnknownHostException {
		SpinnakerLinkData link1 = new SpinnakerLinkData(34, location00,
				Direction.NORTHEAST, createInetAddress());
		SpinnakerLinkData link2 = new SpinnakerLinkData(34, location00,
				Direction.NORTHEAST, createInetAddress());
		assertTrue(link1.sameAs(link2));
		checkSame(link1, link2);
		assertEquals(link1, link1);
	}

	@Test
	public void testDifferent() throws UnknownHostException {
		SpinnakerLinkData link1 = new SpinnakerLinkData(34, location00,
				Direction.NORTHEAST, createInetAddress());
		SpinnakerLinkData link2 = new SpinnakerLinkData(33, location00,
				Direction.NORTHEAST, createInetAddress());
		SpinnakerLinkData link3 = new SpinnakerLinkData(34, location01,
				Direction.NORTHEAST, createInetAddress());
		SpinnakerLinkData link4 = new SpinnakerLinkData(34, location00,
				Direction.NORTH, createInetAddress());
		byte[] bytes = {127, 0, 0, 1};
		InetAddress address2 = InetAddress.getByAddress(bytes);
		SpinnakerLinkData link5 = new SpinnakerLinkData(34, location00,
				Direction.NORTHEAST, address2);

		checkDifferent(link1, link2);
		checkDifferent(link1, link3);
		checkDifferent(link1, link4);
		checkDifferent(link1, link5);

		assertNotEquals(link1, null);
		assertNotEquals(link1, "link1");

	}

	@Test
	@SuppressWarnings("unused")
	public void testBad() throws UnknownHostException {
		assertThrows(IllegalArgumentException.class, () -> {
			SpinnakerLinkData link1 = new SpinnakerLinkData(34, null,
					Direction.NORTHEAST, createInetAddress());
		});
		assertThrows(IllegalArgumentException.class, () -> {
			SpinnakerLinkData link1 = new SpinnakerLinkData(34, location00,
					null, createInetAddress());
		});
		SpinnakerLinkData link1 = new SpinnakerLinkData(34, location00,
				Direction.NORTHEAST, null);
	}
}
