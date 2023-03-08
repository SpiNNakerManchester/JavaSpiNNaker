/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
