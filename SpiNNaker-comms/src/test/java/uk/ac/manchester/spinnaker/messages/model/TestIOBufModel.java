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
package uk.ac.manchester.spinnaker.messages.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.ChipLocation;

class TestIOBufModel {

	@Test
	void test() throws UnsupportedEncodingException {
		byte[] buf = "Everything failed on chip.".getBytes("ASCII");
		IOBuffer b = new IOBuffer(new ChipLocation(1, 2).getScampCore(), buf);
		assertEquals(1, b.getX());
		assertEquals(2, b.getY());
		assertEquals(0, b.getP());
		assertEquals("Everything failed on chip.", b.getContentsString());
	}

}
