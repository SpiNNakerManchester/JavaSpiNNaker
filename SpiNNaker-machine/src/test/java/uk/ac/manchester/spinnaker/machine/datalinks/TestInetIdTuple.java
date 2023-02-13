/*
 * Copyright (c) 2018-2023 The University of Manchester
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
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian-B
 */
public class TestInetIdTuple {
	@Test
	public void testEquals() throws UnknownHostException {
		byte[] bytes1 = {127, 0, 0, 0};
		var addr1 = InetAddress.getByAddress(bytes1);
		var addr2 = InetAddress.getByAddress(bytes1);
		var t1 = new InetIdTuple(addr1, 23);
		var t2 = new InetIdTuple(addr2, 23);

		assertEquals(t1, t1);
		assertEquals(t1, t2);
		assertEquals(t1.hashCode(), t2.hashCode());
	}

	@Test
	public void testEqualsWithNull() {
		var t1 = new InetIdTuple(null, 23);
		var t2 = new InetIdTuple(null, 23);

		assertEquals(t1, t2);
		assertEquals(t1.hashCode(), t2.hashCode());
	}

	@Test
	public void testDifferent() throws UnknownHostException {
		byte[] bytes1 = {127, 0, 0, 0};
		var addr1 = InetAddress.getByAddress(bytes1);
		byte[] bytes2 = {127, 0, 0, 1};
		var addr2 = InetAddress.getByAddress(bytes2);

		var t1 = new InetIdTuple(addr1, 23);
		var t2 = new InetIdTuple(addr2, 23);
		var t3 = new InetIdTuple(null, 23);
		var t4 = new InetIdTuple(addr1, 24);

		assertNotEquals(t1, t2);
		assertNotEquals(t1.hashCode(), t2.hashCode());
		assertNotEquals(t1, t3);
		assertNotEquals(t1.hashCode(), t3.hashCode());
		assertNotEquals(t1, t4);
		assertNotEquals(t1.hashCode(), t4.hashCode());

		assertNotEquals(t3, null);
		assertNotEquals(t1, "t1");

	}

}
