/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.py2json;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.py2json.MachineDefinitionConverter.getJsonWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.EnumSet;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

class TestConvert {
	private static final int BUFFER_SIZE = 1024;

	private String readFile(String filename) throws IOException {
		try (InputStream i =
				getClass().getClassLoader().getResourceAsStream(filename);
				Reader isr = new InputStreamReader(i)) {
			StringBuilder sb = new StringBuilder();
			char[] buffer = new char[BUFFER_SIZE];
			while (true) {
				int len = isr.read(buffer);
				if (len < 0) {
					break;
				}
				sb.append(buffer, 0, len);
			}
			return sb.toString();
		}
	}

	private File getFile(String filename) {
		File f = new File(
				getClass().getClassLoader().getResource(filename).getFile());
		System.setProperty("user.dir", f.getAbsoluteFile().getParent());
		return f;
	}

	@Test
	void testReadPythonSingleBoard() {
		File f = getFile("single_board.py");
		Configuration c;
		try (MachineDefinitionConverter mdl =
				new MachineDefinitionConverter()) {
			c = mdl.loadClassicConfigurationDefinition(f, false);
		}
		Machine m = c.machines.get(0);
		assertNotNull(m);
		assertEquals("192.168.0.2", m.bmpIPs.get(new CF(0, 0)));
		assertEquals(new CFB(0, 0, 0), m.boardLocations.get(new XYZ(0, 0, 0)));
		assertEquals("192.168.0.3", m.spinnakerIPs.get(new XYZ(0, 0, 0)));
		assertEquals("Machine(name=my-board,tags=[default],width=1,height=1,"
				+ "deadBoards=[[x:0,y:0,z:1], [x:0,y:0,z:2]],deadLinks={},"
				+ "boardLocations={[x:0,y:0,z:0]=[c:0,f:0,b:0]},"
				+ "bmpIPs={[c:0,f:0]=192.168.0.2},"
				+ "spinnakerIPs={[x:0,y:0,z:0]=192.168.0.3})", m.toString());
	}

	@Test
	void testReadPythonThreeBoard() {
		File f = getFile("three_board.py");
		Configuration c;
		try (MachineDefinitionConverter mdl =
				new MachineDefinitionConverter()) {
			c = mdl.loadClassicConfigurationDefinition(f, false);
		}
		Machine m = c.machines.get(0);
		assertNotNull(m);
		assertEquals(EnumSet.of(Link.east), m.deadLinks.get(new XYZ(0, 0, 0)));
		assertNotEquals("", m.toString());
	}

	@Test
	void testReadPythonFromCSV() {
		File f = getFile("from_csv.py");
		Configuration c;
		try (MachineDefinitionConverter mdl =
				new MachineDefinitionConverter()) {
			c = mdl.loadClassicConfigurationDefinition(f, true);
		}
		assertNotNull(c.machines.get(0));
	}

	@Test
	void testProduceJSON() throws IOException, JSONException {
		String expectedJson = readFile("expected.json");
		File f = getFile("single_board.py");
		String json;
		try (MachineDefinitionConverter mdl =
				new MachineDefinitionConverter()) {
			Configuration c = mdl.loadClassicConfigurationDefinition(f, true);
			json = getJsonWriter().writeValueAsString(c);
		}
		assertNotNull(json);
		JSONAssert.assertEquals(expectedJson, json, true);
	}
}
