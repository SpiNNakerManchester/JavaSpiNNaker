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
package uk.ac.manchester.spinnaker.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
@Tag("integration")
public class PingITCase {
	@Test
	public void testPingSpalloc() throws UnknownHostException {
		var spalloc = InetAddress.getByName("spinnaker.cs.man.ac.uk");
		// Should be able to reach Spalloc...
		assertEquals(0, Ping.ping(spalloc));
	}

	@Test
	public void testPingGoogle() throws UnknownHostException {
		var travis = InetAddress.getByName("8.8.8.8");
		// *REALLY* should be able to reach Google's DNS...
		assertEquals(0, Ping.ping(travis));
	}

	@Test
	public void testPingLocalhost() {
		// Can't ping localhost? Network catastrophically bad!
		assertEquals(0, Ping.ping("localhost"));
	}

	@Test
	public void testPingDownHost() {
		// CHECKSTYLE:OFF
		/*
		 * Definitely unpingable host
		 * http://answers.microsoft.com/en-us/windows/forum/windows_vista-networking/invalid-ip-address-169254xx/ce096728-e2b7-4d54-80cc-52a4ed342870
		 */
		// CHECKSTYLE:ON
		assertNotEquals(0, Ping.ping("169.254.254.254"));
	}
}
