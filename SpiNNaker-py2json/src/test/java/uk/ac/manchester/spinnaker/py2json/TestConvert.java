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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.ac.manchester.spinnaker.py2json.MachineDefinitionConverter.getJsonWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import uk.ac.manchester.spinnaker.py2json.MachineDefinitionConverter.Configuration;

class TestConvert {
	private static final int BUFFER_SIZE = 1024;

	private String readFile(String filename) throws IOException {
		try (var i = getClass().getClassLoader().getResourceAsStream(filename);
				var isr = new InputStreamReader(i)) {
			var sb = new StringBuilder();
			var buffer = new char[BUFFER_SIZE];
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
		var f = new File(
				getClass().getClassLoader().getResource(filename).getFile());
		System.setProperty("user.dir", f.getAbsoluteFile().getParent());
		return f;
	}

	@Test
	void testReadPythonSingleBoard() {
		var f = getFile("single_board.py");
		Configuration c;
		try (var mdl = new MachineDefinitionConverter()) {
			c = mdl.loadClassicConfigurationDefinition(f, false);
		}
		assertNotNull(c.machines.get(0));
	}

	@Test
	void testReadPythonFromCSV() {
		var f = getFile("from_csv.py");
		Configuration c;
		try (var mdl = new MachineDefinitionConverter()) {
			c = mdl.loadClassicConfigurationDefinition(f, true);
		}
		assertNotNull(c.machines.get(0));
	}

	@Test
	void testProduceJSON() throws IOException, JSONException {
		var expectedJson = readFile("expected.json");
		var f = getFile("single_board.py");
		String json;
		try (var mdl = new MachineDefinitionConverter()) {
			var c = mdl.loadClassicConfigurationDefinition(f, true);
			json = getJsonWriter().writeValueAsString(c);
		}
		assertNotNull(json);
		JSONAssert.assertEquals(expectedJson, json, true);
	}
}
