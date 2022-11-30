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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author Christain
 */
public class TestConnection {

	@Test
	void testFromJson() throws IOException {
		var json = "[[2,4],\"6.8.10.12\"]";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, Connection.class);
		assertEquals(new ChipLocation(2, 4), fromJson.chip());
		assertEquals("6.8.10.12", fromJson.hostname());

		var direct = new Connection(new ChipLocation(2, 4), "6.8.10.12");
		assertEquals(direct, fromJson);
		assertEquals(direct.hashCode(), fromJson.hashCode());
		assertEquals(direct.toString(), fromJson.toString());
	}

	@Test
	void testNulls() throws IOException {
		var json = "[null,null]";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, Connection.class);
		assertNull(fromJson.chip());
		assertNull(fromJson.hostname());

		var direct = new Connection(null, null);
		assertEquals(direct, fromJson);
		assertEquals(direct.hashCode(), fromJson.hashCode());
		assertEquals(direct.toString(), fromJson.toString());
	}

}
