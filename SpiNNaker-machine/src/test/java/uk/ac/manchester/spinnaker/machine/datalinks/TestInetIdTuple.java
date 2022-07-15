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

/**
 *
 * @author Christian-B
 */
public class TestInetIdTuple {

	public TestInetIdTuple() {
	}

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
	public void testEqualsWithNull() throws UnknownHostException {
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
