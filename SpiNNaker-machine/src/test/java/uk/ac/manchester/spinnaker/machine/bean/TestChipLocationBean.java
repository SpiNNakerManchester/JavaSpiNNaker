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
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 *
 * @author Christian
 */
public class TestChipLocationBean {

	@Test
	public void testFromJson() throws IOException {
		var json = "[2, 4]";
		var mapper = MapperFactory.createMapper();
		var fromJson = mapper.readValue(json, ChipLocation.class);
		assertEquals(2, fromJson.getX());
		assertEquals(4, fromJson.getY());

		var direct = new ChipLocation(2, 4);
		assertEquals(direct, fromJson);
		assertEquals(direct.hashCode(), fromJson.hashCode());
		assertEquals(direct.toString(), fromJson.toString());
	}

	@Test
	public void testNullJson() throws IOException {
		var json = "null";
		var mapper = MapperFactory.createMapper();
		var fromJson = mapper.readValue(json, ChipLocation.class);
		assertNull(fromJson);
	}

	@Test
	public void testOneNullJson() throws IOException {
		var json = "[2]";
		var mapper = MapperFactory.createMapper();
		assertThrows(MismatchedInputException.class, () -> {
			mapper.readValue(json, ChipLocation.class);
		});
	}
}
