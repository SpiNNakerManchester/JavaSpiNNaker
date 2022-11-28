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

import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian
 */
public class TestChipDetails {
	@Test
	public void testFromJson() throws IOException {
		var json = """
				{
					"cores": 18,
					"deadLinks": [3, 4, 5],
					"ipAddress": "130.88.192.243",
					"ethernet": [0, 0]
				}
				""";
		/*
		 * String json = "{\"cores\": 18, \"ipAddress\": \"130.88.192.243\",
		 * \"ethernet\":[0, 0]}";
		 */
		var mapper = MapperFactory.createMapper();
		var fromJson = mapper.readValue(json, ChipDetails.class);
		assertNotNull(fromJson);
	}
}
