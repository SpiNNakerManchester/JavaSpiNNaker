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
public class TestAbstractDataLink {

	private ChipLocation location00 = new ChipLocation(0, 0);

	private InetAddress createInetAddress() throws UnknownHostException {
		byte[] bytes = {127, 0, 0, 0};
		return InetAddress.getByAddress(bytes);
	}

	public TestAbstractDataLink() {
	}

	@Test
	public void testEquals() throws UnknownHostException {
		AbstractDataLink link1 = new AbstractDataLink(location00,
				Direction.NORTHEAST, createInetAddress());
		AbstractDataLink link2 = new AbstractDataLink(location00,
				Direction.NORTHEAST, createInetAddress());
		assertEquals(link1, link2);
		assertEquals(link1, link1);
		assertNotEquals(link1, null);
		assertNotEquals(link1, "link1");
	}

}
