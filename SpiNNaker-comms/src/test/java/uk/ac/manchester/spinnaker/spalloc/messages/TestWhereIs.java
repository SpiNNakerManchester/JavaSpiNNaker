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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author micro
 */
public class TestWhereIs {

	@Test
	void testFromJson() throws IOException {
		var jobChip = new ChipLocation(1, 2);
		var chip = new ChipLocation(3, 4);
		var boardChip = new ChipLocation(8, 9);
		var logical = new BoardCoordinates(5, 6, 7);
		var physical = new BoardPhysicalCoordinates(10, 11, 12);

		var json = """
				{
					"job_chip": [1,2],
					"job_id": 666,
					"chip": [3,4],
					"logical": [5,6,7],
					"machine": "Spin24b-001",
					"board_chip": [8,9],
					"physical": [10,11,12]
				}
				""";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, WhereIs.class);
		assertEquals(jobChip, fromJson.jobChip());
		assertEquals(666, fromJson.jobId());
		assertEquals(chip, fromJson.chip());
		assertEquals(logical, fromJson.logical());
		assertEquals("Spin24b-001", fromJson.machine());
		assertEquals(boardChip, fromJson.boardChip());
		var physical2 = fromJson.physical();
		assertEquals(physical, physical2);

		var direct = new WhereIs(jobChip, 666, chip, logical, "Spin24b-001",
				boardChip, physical);
		assertEquals(direct, fromJson);
		// assertEquals(direct.hashCode(), fromJson.hashCode());
		assertEquals(direct.toString(), fromJson.toString());
	}

	@Test
	void testBug() throws IOException {
		var chip = new ChipLocation(8, 4);
		var boardChip = new ChipLocation(0, 0);
		var logical = new BoardCoordinates(0, 0, 1);
		var physical = new BoardPhysicalCoordinates(0, 0, 8);

		var json = """
				{
					"job_chip": null,
					"job_id": null,
					"chip": [8,4],
					"logical": [0,0,1],
					"machine": "Spin24b-001",
					"board_chip": [0,0],
					"physical": [0,0,8]
				}
				""";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, WhereIs.class);
		assertNull(fromJson.jobChip());
		assertNull(fromJson.jobId());
		assertEquals(chip, fromJson.chip());
		assertEquals(logical, fromJson.logical());
		assertEquals("Spin24b-001", fromJson.machine());
		assertEquals(boardChip, fromJson.boardChip());
		assertEquals(physical, fromJson.physical());

		var direct = new WhereIs(null, null, chip, logical, "Spin24b-001",
				boardChip, physical);
		assertEquals(direct, fromJson);
		// assertEquals(direct.hashCode(), fromJson.hashCode());
		assertEquals(direct.toString(), fromJson.toString());
	}

	@Test
	void testNulls() throws IOException {
		var json = """
				{
					"job_chip": null,
					"job_id": null,
					"chip": null,
					"logical": null,
					"machine": null,
					"board_chip": null,
					"physical": null
				}
				""";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, WhereIs.class);
		assertNull(fromJson.jobChip());
		assertNull(fromJson.jobId());
		assertNull(fromJson.chip());
		assertNull(fromJson.logical());
		assertNull(fromJson.machine());
		assertNull(fromJson.boardChip());
		assertNull(fromJson.physical());

		var direct = new WhereIs(null, null, null, null, null, null, null);
		assertEquals(direct, fromJson);
		// assertEquals(direct.hashCode(), fromJson.hashCode());
		assertEquals(direct.toString(), fromJson.toString());
	}
}
