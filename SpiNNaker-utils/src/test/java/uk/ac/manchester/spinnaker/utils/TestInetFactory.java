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

import java.net.UnknownHostException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import uk.ac.manchester.spinnaker.utils.InetFactory.Inet6NotSupportedException;

/**
 *
 * @author Christian-B
 */
public class TestInetFactory {

	public TestInetFactory() {
	}

	@Test
	public void testByBytes() throws UnknownHostException {
		byte[] bytes = {1, 2, 3, 4};
		InetFactory.getByAddress(bytes);
	}

	public void testByName() throws UnknownHostException {
		InetFactory.getByName("apt.cs.manchester.ac.uk");
	}

	@Test
	public void testByBytes6() {
		byte[] bytes = {
			1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
		};
		assertThrows(Inet6NotSupportedException.class, () -> {
			InetFactory.getByAddress(bytes);
		});
	}

	@Test
	public void testByName6() {
		String bytes = "3731:54:65fe:2::a7";
		assertThrows(Inet6NotSupportedException.class, () -> {
			InetFactory.getByName(bytes);
		});
	}

}
