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
 * @author micro
 */
public class TestWhereIs {

	void testFromJson() throws IOException {
		ChipLocation jobChip = new ChipLocation(1, 2);
		ChipLocation chip = new ChipLocation(3, 4);
		ChipLocation boardChip = new ChipLocation(8, 9);
		BoardCoordinates logical = new BoardCoordinates(5, 6, 7);
		BoardPhysicalCoordinates physical =
				new BoardPhysicalCoordinates(10, 11, 12);

		String json = "{\"job_chip\":[1,2],\"job_id\":666,\"chip\":[3,4],"
				+ "\"logical\":[5,6,7],\"machine\":\"Spin24b-001\","
				+ "\"board_chip\":[8,9],\"physical\":[10,11,12]}";
		ObjectMapper mapper = SpallocClient.createMapper();
		WhereIs fromJson = mapper.readValue(json, WhereIs.class);
		assertEquals(jobChip, fromJson.getJobChip());
		assertEquals(666, fromJson.getJobId());
		assertEquals(chip, fromJson.getChip());
		assertEquals(logical, fromJson.getLogical());
		assertEquals("Spin24b-001", fromJson.getMachine());
		assertEquals(boardChip, fromJson.getBoardChip());
		assertEquals(boardChip, fromJson.getPhysical());

		WhereIs direct = new WhereIs(jobChip, 666, chip, logical, "Spin24b-001",
				boardChip, physical);
		assertEquals(direct, fromJson);
		// assertEquals(direct.hashCode(), fromJson.hashCode());
		assertEquals(direct.toString(), fromJson.toString());
	}

	@Test
	void testBug() throws IOException {
		ChipLocation chip = new ChipLocation(8, 4);
		ChipLocation boardChip = new ChipLocation(0, 0);
		BoardCoordinates logical = new BoardCoordinates(0, 0, 1);
		BoardPhysicalCoordinates physical =
				new BoardPhysicalCoordinates(0, 0, 8);

		String json = "{\"job_chip\":null,\"job_id\":null,\"chip\":[8,4],"
				+ "\"logical\":[0,0,1],\"machine\":\"Spin24b-001\","
				+ "\"board_chip\":[0,0],\"physical\":[0,0,8]}";
		ObjectMapper mapper = SpallocClient.createMapper();
		WhereIs fromJson = mapper.readValue(json, WhereIs.class);
		assertNull(fromJson.getJobChip());
		assertEquals(0, fromJson.getJobId());
		assertEquals(chip, fromJson.getChip());
		assertEquals(logical, fromJson.getLogical());
		assertEquals("Spin24b-001", fromJson.getMachine());
		assertEquals(boardChip, fromJson.getBoardChip());
		assertEquals(physical, fromJson.getPhysical());

		WhereIs direct = new WhereIs(null, 0, chip, logical, "Spin24b-001",
				boardChip, physical);
		assertEquals(direct, fromJson);
		// assertEquals(direct.hashCode(), fromJson.hashCode());
		assertEquals(direct.toString(), fromJson.toString());
	}

	@Test
	void testNulls() throws IOException {

		String json = "{\"job_chip\":null,\"job_id\":null,\"chip\":null,"
				+ "\"logical\":null,\"machine\":null,\"board_chip\":null,"
				+ "\"physical\":null}";
		ObjectMapper mapper = SpallocClient.createMapper();
		WhereIs fromJson = mapper.readValue(json, WhereIs.class);
		assertNull(fromJson.getJobChip());
		assertEquals(0, fromJson.getJobId());
		assertNull(fromJson.getChip());
		assertNull(fromJson.getLogical());
		assertNull(fromJson.getMachine());
		assertNull(fromJson.getBoardChip());
		assertNull(fromJson.getPhysical());

		WhereIs direct = new WhereIs(null, 0, null, null, null, null, null);
		assertEquals(direct, fromJson);
		// assertEquals(direct.hashCode(), fromJson.hashCode());
		assertEquals(direct.toString(), fromJson.toString());
	}
}
