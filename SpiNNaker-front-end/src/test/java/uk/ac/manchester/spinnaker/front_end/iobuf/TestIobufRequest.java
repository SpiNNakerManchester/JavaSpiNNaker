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

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;

public class TestIobufRequest {

	@Test
	public void testCorrect() throws IOException {
		String input = "{\n"
				+ "  \"/some/path/abc.aplx\": [ [0,0,1], [0,0,2], [0,0,3] ],\n"
				+ "  \"/some/path/def.aplx\": [ [0,1,1], [0,1,2], [0,1,3] ],\n"
				+ "  \"/some/path/ghi.aplx\": [ [1,0,1], [1,0,2], [0,0,4] ]\n"
				+ "}";

		ObjectMapper mapper = MapperFactory.createMapper();
		IobufRequest req = mapper.readValue(input, IobufRequest.class);
		Map<File, CoreSubsets> details = req.getRequestDetails();

		File f1 = (new File("/some/path/abc.aplx")).getAbsoluteFile();
		File f2 = (new File("/some/path/def.aplx")).getAbsoluteFile();
		File f3 = (new File("/some/path/ghi.aplx")).getAbsoluteFile();
		assertEquals(new HashSet<>(asList(f1, f2, f3)), details.keySet());
		assertEquals(new HashSet<>(asList(new ChipLocation(0, 0))),
				details.get(f1).getChips());
		assertEquals(new HashSet<>(asList(1, 2, 3)),
				details.get(f1).pByChip(new ChipLocation(0, 0)));
		assertEquals(new HashSet<>(asList(new ChipLocation(0, 1))),
				details.get(f2).getChips());
		assertEquals(
				new HashSet<>(
						asList(new ChipLocation(0, 0), new ChipLocation(1, 0))),
				details.get(f3).getChips());
	}

	@Test
	public void testIncorrect() throws IOException {
		// Overlap between the core subsets; THIS IS BAD and shouldn't happen
		String input = "{\n"
				+ "  \"/some/path/abc.aplx\": [ [0,0,1], [0,0,2], [0,0,3] ],\n"
				+ "  \"/some/path/def.aplx\": [ [0,1,1], [0,1,2], [0,0,3] ],\n"
				+ "  \"/some/path/ghi.aplx\": [ [0,0,1], [1,0,2], [0,0,4] ]\n"
				+ "}";

		ObjectMapper mapper = MapperFactory.createMapper();
		Exception e = assertThrows(InvalidDefinitionException.class,
				() -> mapper.readValue(input, IobufRequest.class));
		assertNotNull(e.getCause());
		assertEquals(IllegalArgumentException.class, e.getCause().getClass());
		assertEquals("overlapping uses of core", e.getCause().getMessage());
	}
}
