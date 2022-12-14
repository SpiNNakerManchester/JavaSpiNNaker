/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.iobuf;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;

import org.junit.jupiter.api.Test;

public class TestReplacer {
	private URL aplx = getClass().getResource("empty.aplx");

	@Test
	public void replace() {
		// Ignores bad line in dict file
		var replacer = new Replacer(aplx.getFile());

		assertEquals("abc-d-2-e-3-f-4",
				replacer.replace("1\u001e2\u001e3\u001e4"));
		assertEquals("abc,d,1,e,3,f,4",
				replacer.replace("2\u001e1\u001e3\u001e4"));
		assertEquals("abc,d,12345,e,34567,f,45678",
				replacer.replace("2\u001e12345\u001e34567\u001e45678"));
		assertEquals("1,2,3,4", replacer.replace("1,2,3,4"));
	}
}
