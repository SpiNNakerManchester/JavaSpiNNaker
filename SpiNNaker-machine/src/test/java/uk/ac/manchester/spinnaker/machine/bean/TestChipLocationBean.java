/*
 * Copyright (c) 2018 The University of Manchester
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
	public void testOneNullJson() {
		var json = "[2]";
		var mapper = MapperFactory.createMapper();
		assertThrows(MismatchedInputException.class, () -> {
			mapper.readValue(json, ChipLocation.class);
		});
	}
}
