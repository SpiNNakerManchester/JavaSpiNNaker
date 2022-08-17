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
import static uk.ac.manchester.spinnaker.machine.Direction.NORTH;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTHEAST;

import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 *
 * @author Christian-B
 */
public class TestSpinnakerLinkData {

	private static final ChipLocation CHIP00 = new ChipLocation(0, 0);

	private static final ChipLocation CHIP01 = new ChipLocation(0, 1);

	private static InetAddress createInetAddress() throws UnknownHostException {
		byte[] bytes = {127, 0, 0, 0};
		return InetAddress.getByAddress(bytes);
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
		var link1 = new SpinnakerLinkData(34, CHIP00, NORTHEAST,
				createInetAddress());
		var link2 = new SpinnakerLinkData(34, CHIP00, NORTHEAST,
				createInetAddress());
		assertTrue(link1.sameAs(link2));
		checkSame(link1, link2);
		assertEquals(link1, link1);
	}

	@Test
	public void testDifferent() throws UnknownHostException {
		var link1 = new SpinnakerLinkData(34, CHIP00, NORTHEAST,
				createInetAddress());
		var link2 = new SpinnakerLinkData(33, CHIP00, NORTHEAST,
				createInetAddress());
		var link3 = new SpinnakerLinkData(34, CHIP01, NORTHEAST,
				createInetAddress());
		var link4 =
				new SpinnakerLinkData(34, CHIP00, NORTH, createInetAddress());
		byte[] bytes = {127, 0, 0, 1};
		var address2 = InetAddress.getByAddress(bytes);
		var link5 = new SpinnakerLinkData(34, CHIP00, NORTHEAST, address2);

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
			var link1 = new SpinnakerLinkData(34, null, NORTHEAST,
					createInetAddress());
		});
		assertThrows(IllegalArgumentException.class, () -> {
			var link1 = new SpinnakerLinkData(34, CHIP00, null,
					createInetAddress());
		});
		var link1 = new SpinnakerLinkData(34, CHIP00, NORTHEAST, null);
	}
}
