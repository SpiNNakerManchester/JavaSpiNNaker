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
		 * https://answers.microsoft.com/en-us/windows/forum/windows_vista-networking/invalid-ip-address-169254xx/ce096728-e2b7-4d54-80cc-52a4ed342870
		 */
		// CHECKSTYLE:ON
		assertNotEquals(0, Ping.ping("169.254.254.254"));
	}
}
