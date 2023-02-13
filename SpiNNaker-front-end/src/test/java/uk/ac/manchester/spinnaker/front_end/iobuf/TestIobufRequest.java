/*
 * Copyright (c) 2019-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.front_end.iobuf;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.bean.MapperFactory.createMapper;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

import uk.ac.manchester.spinnaker.machine.ChipLocation;

public class TestIobufRequest {

	@Test
	public void testCorrect() throws IOException {
		var input = "{\n"
				+ "  \"/some/path/abc.aplx\": [ [0,0,1], [0,0,2], [0,0,3] ],\n"
				+ "  \"/some/path/def.aplx\": [ [0,1,1], [0,1,2], [0,1,3] ],\n"
				+ "  \"/some/path/ghi.aplx\": [ [1,0,1], [1,0,2], [0,0,4] ]\n"
				+ "}";

		var req = createMapper().readValue(input, IobufRequest.class);
		var details = req.getRequestDetails();

		var f1 = new File("/some/path/abc.aplx").getAbsoluteFile();
		var f2 = new File("/some/path/def.aplx").getAbsoluteFile();
		var f3 = new File("/some/path/ghi.aplx").getAbsoluteFile();
		assertEquals(Set.of(f1, f2, f3), details.keySet());
		assertEquals(Set.of(new ChipLocation(0, 0)),
				details.get(f1).getChips());
		assertEquals(Set.of(1, 2, 3),
				details.get(f1).pByChip(new ChipLocation(0, 0)));
		assertEquals(Set.of(new ChipLocation(0, 1)),
				details.get(f2).getChips());
		assertEquals(Set.of(new ChipLocation(0, 0), new ChipLocation(1, 0)),
				details.get(f3).getChips());
	}

	@Test
	public void testIncorrect() {
		// Overlap between the core subsets; THIS IS BAD and shouldn't happen
		var input = "{\n"
				+ "  \"/some/path/abc.aplx\": [ [0,0,1], [0,0,2], [0,0,3] ],\n"
				+ "  \"/some/path/def.aplx\": [ [0,1,1], [0,1,2], [0,0,3] ],\n"
				+ "  \"/some/path/ghi.aplx\": [ [0,0,1], [1,0,2], [0,0,4] ]\n"
				+ "}";

		var e = assertThrows(ValueInstantiationException.class,
				() -> createMapper().readValue(input, IobufRequest.class));
		assertNotNull(e.getCause());
		assertEquals(IllegalArgumentException.class, e.getCause().getClass());
		assertEquals("overlapping uses of core", e.getCause().getMessage());
	}
}
