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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author Christian
 */
public class TestJobMachineInfo {
	@Test
	void testFromJson() throws IOException {
		var json = """
				{
					"connections": [
						[[0,0], "10.2.225.177"],
						[[4,8], "10.2.225.145"],
						[[0,12], "10.2.225.185"],
						[[8,16], "10.2.225.121"],
						[[4,20], "10.2.225.153"],
						[[8,4], "10.2.225.113"]
					],
					"width": 16,
					"machine_name": "Spin24b-001",
					"boards": [
						[2,1,1],
						[2,1,0],
						[2,1,2],
						[2,0,2],
						[2,0,1],
						[2,0,0]
					],
					"height": 24
				}
				""";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, JobMachineInfo.class);
		assertEquals(6, fromJson.getConnections().size());
		assertEquals(6, fromJson.getBoards().size());
		assertEquals(16, fromJson.getWidth());
		assertEquals(24, fromJson.getHeight());
		assertEquals("Spin24b-001", fromJson.getMachineName());
	}

	@Test
	void testNullJson() throws IOException {
		var json = """
				{
					"connections": null,
					"machine_name": null,
					"boards": null
				}
				""";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, JobMachineInfo.class);
		assertEquals(0, fromJson.getConnections().size());
		assertEquals(0, fromJson.getBoards().size());
		assertEquals(0, fromJson.getWidth());
		assertEquals(0, fromJson.getHeight());
		assertNull(fromJson.getMachineName());
		assertNotNull(fromJson.toString());
	}
}
