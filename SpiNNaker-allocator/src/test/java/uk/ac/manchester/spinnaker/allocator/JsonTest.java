/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.allocator;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.allocator.SpallocClientFactory.JSON_MAPPER;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.messages.model.Version;

class JsonTest {
	private String serialize(Object obj) throws IOException {
		return JSON_MAPPER.writeValueAsString(obj);
	}

	private <T> T deserialize(String string, Class<T> cls) throws IOException {
		return JSON_MAPPER.readValue(string, cls);
	}

	@Test
	void allocatedMachine() throws Exception {
		assertNotNull(
				deserialize(serialize(new AllocatedMachine.Builder().build()),
						AllocatedMachine.class));
	}

	@Test
	void boardCoords() throws Exception {
		var bc = new BoardCoords(0, 1, 2, 3, 4, 5, "6.7.8.9");
		assertNotNull(serialize(bc));
		assertEquals(bc, deserialize(serialize(bc), BoardCoords.class));
	}

	@Test
	void createJob() throws Exception {
		assertNotNull(
				deserialize(serialize(new CreateJob(1)), CreateJob.class));
	}

	@Test
	void jobDescription() throws Exception {
		assertNotNull(
				deserialize(serialize(new JobDescription.Builder().build()),
						JobDescription.class));
	}

	@Test
	void jobs() throws Exception {
		// Deserialize only
		var jobs = "{\"jobs\":[\"http:foo1\",\"http:foo2\"],"
				+ "\"prev\":\"http:foo3\",\"next\":\"http:foo4\"}";
		assertEquals(2, deserialize(jobs, Jobs.class).jobs.size());
	}

	@Test
	void machines() throws Exception {
		// Deserialize only
		// also BriefMachineDescription, DeadLink and Direction
		var ms = "{\"machines\":[{\"name\":\"foo\",\"dead-links\":[["
				+ "{\"board\":{\"x\":0,\"y\":0,\"z\":0},\"direction\":0},"
				+ "{\"board\":{\"x\":0,\"y\":0,\"z\":0},\"direction\":1}]]}]}";
		assertEquals(1, deserialize(ms, Machines.class).machines.size());
	}

	@Test
	void power() throws Exception {
		var power = "{\"power\":\"OFF\"}";
		assertNotNull(deserialize(power, Power.class));
	}

	@Test
	void rootInfo() throws Exception {
		var rootInfo = "{\"csrf-header\":\"a\",\"csrf-token\":\"b\","
				+ "\"jobs-uri\":\"http:c\",\"machines-uri\":\"http:d\","
				+ "\"version\":\"1.2.3\"}";
		var ri = deserialize(rootInfo, RootInfo.class);
		assertNotNull(ri);
		assertEquals("a", ri.csrfHeader);
		assertEquals("b", ri.csrfToken);
		assertEquals("c", ri.jobsURI.getSchemeSpecificPart());
		assertEquals("d", ri.machinesURI.getSchemeSpecificPart());
		assertEquals(new Version(1, 2, 3), ri.version);
	}

	@Test
	void whereis() throws Exception {
		var whereIs = "{\"job-id\":123,\"job-ref\":\"http:/0\","
				+ "\"job-chip\":[0,0],\"chip\":[1,1],"
				+ "\"machine-name\":\"gorp\",\"machine-ref\":\"http:/1\","
				+ "\"board-chip\":[2,2],\"logical-board-coordinates\":[0,1,2],"
				+ "\"physical-board-coordinates\":[3,4,5]}";
		var wi = deserialize(whereIs, WhereIs.class);

		assertNotNull(wi);
		assertEquals("gorp", wi.getMachineName());
		assertEquals(URI.create("http:/1"), wi.getMachineRef());
		assertEquals(123, wi.getJobId());
		assertEquals(URI.create("http:/0"), wi.getJobRef());
		assertEquals(new ChipLocation(0, 0), wi.getJobChip());
		assertEquals(new ChipLocation(1, 1), wi.getChip());
		assertEquals(new ChipLocation(2, 2), wi.getBoardChip());
		assertEquals(new TriadCoords(0, 1, 2), wi.getLogicalCoords());
		assertEquals(new PhysicalCoords(3, 4, 5), wi.getPhysicalCoords());
	}
}
