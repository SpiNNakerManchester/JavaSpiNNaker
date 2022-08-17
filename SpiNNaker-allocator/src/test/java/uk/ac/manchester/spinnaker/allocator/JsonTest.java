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

import org.junit.jupiter.api.Test;

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
		assertNotNull(deserialize(serialize(new AllocatedMachine()),
				AllocatedMachine.class));
	}

	@Test
	void boardCoords() throws Exception {
		BoardCoords bc = new BoardCoords(0, 1, 2, 3, 4, 5, "6.7.8.9");
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
		assertNotNull(deserialize(serialize(new JobDescription()),
				JobDescription.class));
	}

	@Test
	void jobs() throws Exception {
		// Deserialize only
		String jobs = "{\"jobs\":[\"http:foo1\",\"http:foo2\"],"
				+ "\"prev\":\"http:foo3\",\"next\":\"http:foo4\"}";
		assertEquals(2, deserialize(jobs, Jobs.class).jobs.size());
	}

	@Test
	void machines() throws Exception {
		// Deserialize only
		// also BriefMachineDescription, DeadLink and Direction
		String ms = "{\"machines\":[{\"name\":\"foo\",\"dead-links\":[["
				+ "{\"board\":{\"x\":0,\"y\":0,\"z\":0},\"direction\":0},"
				+ "{\"board\":{\"x\":0,\"y\":0,\"z\":0},\"direction\":1}]]}]}";
		assertEquals(1, deserialize(ms, Machines.class).machines.size());
	}

	@Test
	void physical() throws Exception {
		Physical p = new Physical();
		p.setCabinet(1);
		p.setFrame(2);
		p.setBoard(3);
		assertNotNull(serialize(p));
		assertEquals(p.toString(),
				deserialize(serialize(p), Physical.class).toString());
	}

	@Test
	void power() throws Exception {
		String power = "{\"power\":\"OFF\"}";
		assertNotNull(deserialize(power, Power.class));
	}

	@Test
	void rootInfo() throws Exception {
		String rootInfo = "{\"csrf-header\":\"a\",\"csrf-token\":\"b\","
				+ "\"jobs-uri\":\"http:c\",\"machines-uri\":\"http:d\","
				+ "\"version\":\"1.2.3\"}";
		RootInfo ri = deserialize(rootInfo, RootInfo.class);
		assertNotNull(ri);
		assertEquals("a", ri.csrfHeader);
		assertEquals("b", ri.csrfToken);
		assertEquals("c", ri.jobsURI.getSchemeSpecificPart());
		assertEquals("d", ri.machinesURI.getSchemeSpecificPart());
		assertEquals(new Version(1, 2, 3), ri.version);
	}

	@Test
	void triad() throws Exception {
		Triad t = new Triad();
		t.setX(1);
		t.setY(2);
		t.setZ(3);
		assertNotNull(serialize(t));
		assertEquals(t.toString(),
				deserialize(serialize(t), Triad.class).toString());
	}

}