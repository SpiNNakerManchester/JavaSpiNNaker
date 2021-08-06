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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
		String json = "[[2,4],\"6.8.10.12\"]";
		ObjectMapper mapper = SpallocClient.createMapper();
		Connection fromJson = mapper.readValue(json, Connection.class);
		assertEquals(new ChipLocation(2, 4), fromJson.getChip());
		assertEquals("6.8.10.12", fromJson.getHostname());

		Connection direct = new Connection(new ChipLocation(2, 4), "6.8.10.12");
		assertEquals(direct, fromJson);
		assertEquals(direct.hashCode(), fromJson.hashCode());
		assertEquals(direct.toString(), fromJson.toString());
	}

	@Test
	void testNulls() throws IOException {
		String json = "[null,null]";
		ObjectMapper mapper = SpallocClient.createMapper();
		Connection fromJson = mapper.readValue(json, Connection.class);
		assertNull(fromJson.getChip());
		assertNull(fromJson.getHostname());

		Connection direct = new Connection(null, null);
		assertEquals(direct, fromJson);
		assertEquals(direct.hashCode(), fromJson.hashCode());
		assertEquals(direct.toString(), fromJson.toString());
	}

}
