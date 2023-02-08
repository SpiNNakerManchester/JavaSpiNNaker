/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine.datalinks;

import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTH;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTHEAST;
import static uk.ac.manchester.spinnaker.machine.datalinks.FpgaId.TOP_RIGHT;

import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;

/**
 *
 * @author Christian-B
 */
public class TestFpgaLinkData {

	private static ChipLocation location00 = new ChipLocation(0, 0);

	private static ChipLocation location01 = new ChipLocation(0, 1);

	private static InetAddress localhost() throws UnknownHostException {
		byte[] bytes = {127, 0, 0, 1};
		return InetAddress.getByAddress(bytes);
	}

	private void checkDifferent(FPGALinkData link1, FPGALinkData link2) {
		assertNotEquals(link1, link2);
		assertNotEquals(link1.hashCode(), link2.hashCode());
		assertNotEquals(link1.toString(), link2.toString());
	}

	private void checkSame(FPGALinkData link1, FPGALinkData link2) {
		assertEquals(link1, link2);
		assertEquals(link1.hashCode(), link2.hashCode());
		assertEquals(link1.toString(), link2.toString());
	}

	@Test
	public void testEquals() throws UnknownHostException {
		var link1 = new FPGALinkData(34, TOP_RIGHT, location00, NORTHEAST,
				localhost());
		var link2 = new FPGALinkData(34, FpgaId.byId(2), location00, NORTHEAST,
				localhost());
		assertTrue(link1.sameAs(link2));
		checkSame(link1, link2);
		assertEquals(link1, link1);
		assertEquals(ChipLocation.ZERO_ZERO, link1.asChipLocation());
	}

	@Test
	public void testDifferent() throws UnknownHostException {
		var link1 = new FPGALinkData(34, TOP_RIGHT, location00, NORTHEAST,
				localhost());
		var link2 = new FPGALinkData(33, TOP_RIGHT, location00, NORTHEAST,
				localhost());
		var link3 = new FPGALinkData(34, TOP_RIGHT, location01, NORTHEAST,
				localhost());
		var link4 =
				new FPGALinkData(34, TOP_RIGHT, location00, NORTH, localhost());
		byte[] bytes = {127, 0, 0, 2};
		var address2 = InetAddress.getByAddress(bytes);
		var link5 = new FPGALinkData(34, TOP_RIGHT, location00, NORTHEAST,
				address2);
		var link6 = new FPGALinkData(34, FpgaId.LEFT, location00,
				Direction.NORTHEAST, localhost());

		checkDifferent(link1, link2);
		checkDifferent(link1, link3);
		checkDifferent(link1, link4);
		checkDifferent(link1, link5);
		checkDifferent(link1, link6);

		assertNotEquals(link1, null);
		assertNotEquals(link1, "link1");
	}

}
