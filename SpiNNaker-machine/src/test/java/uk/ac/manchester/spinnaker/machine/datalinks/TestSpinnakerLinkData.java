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

	private static InetAddress localhost() throws UnknownHostException {
		byte[] bytes = {127, 0, 0, 1};
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
		var link1 = new SpinnakerLinkData(34, CHIP00, NORTHEAST, localhost());
		var link2 = new SpinnakerLinkData(34, CHIP00, NORTHEAST, localhost());
		assertTrue(link1.sameAs(link2));
		checkSame(link1, link2);
		assertEquals(link1, link1);
	}

	@Test
	public void testDifferent() throws UnknownHostException {
		var link1 = new SpinnakerLinkData(34, CHIP00, NORTHEAST, localhost());
		var link2 = new SpinnakerLinkData(33, CHIP00, NORTHEAST, localhost());
		var link3 = new SpinnakerLinkData(34, CHIP01, NORTHEAST, localhost());
		var link4 = new SpinnakerLinkData(34, CHIP00, NORTH, localhost());
		byte[] bytes = {127, 0, 0, 2};
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
	public void testBad() {
		assertThrows(IllegalArgumentException.class, () -> {
			new SpinnakerLinkData(34, null, NORTHEAST, localhost());
		});
		assertThrows(IllegalArgumentException.class, () -> {
			new SpinnakerLinkData(34, CHIP00, null, localhost());
		});
		assertNotNull(new SpinnakerLinkData(34, CHIP00, NORTHEAST, null));
	}
}
