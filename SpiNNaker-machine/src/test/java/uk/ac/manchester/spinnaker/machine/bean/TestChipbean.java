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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian
 */
public class TestChipbean {
	@Test
	public void testFromJson() throws IOException {
		String json = "[1, 2, {\"cores\": 17, \"ethernet\": [2, 3]}, {"
				+ "\"sdram\": 123469692, "
				+ "\"routerEntries\": 1013, \"monitors\": 2, "
				+ "\"virtual\": true}]";
		ObjectMapper mapper = MapperFactory.createMapper();
		ChipBean fromJson = mapper.readValue(json, ChipBean.class);
		assertNotNull(fromJson);
		System.out.println(fromJson);
	}
}
