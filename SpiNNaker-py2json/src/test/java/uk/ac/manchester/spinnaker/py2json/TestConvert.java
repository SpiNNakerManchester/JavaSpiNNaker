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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.ac.manchester.spinnaker.py2json.MachineDefinitionConverter.getJsonWriter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

class TestConvert {
	/**
	 * Shortening.
	 *
	 * @param filename
	 *            The local filename of the (test) resource.
	 * @return The URL of the resource.
	 */
	private static URL getResource(String filename) {
		return TestConvert.class.getResource(filename);
	}

	/**
	 * Get a resource as a file handle. <strong>Also sets {@code user.dir} to
	 * the directory containing the file!</strong> Without that, the Python code
	 * can't find any auxiliary files, such as the CSV.
	 *
	 * @param filename
	 *            The local filename of the (test) resource.
	 * @return The handle of the file.
	 */
	private static File getFile(String filename) {
		var f = new File(getResource(filename).getFile());
		// Ugh!
		System.setProperty("user.dir", f.getAbsoluteFile().getParent());
		return f;
	}

	private static final String SINGLE_BOARD = "single_board.py";

	private static final String CSV_DEFINED = "from_csv.py";

	private static final String EXPECTED_JSON = "expected.json";

	@Test
	void testReadPythonSingleBoard() {
		var f = getFile(SINGLE_BOARD);
		try (var mdl = new MachineDefinitionConverter()) {
			var c = mdl.loadClassicConfigurationDefinition(f, false);
			assertEquals(1, c.machines.size());
			var machine = c.machines.get(0);
			assertNotNull(machine);
			assertEquals("my-board", machine.name);
			assertEquals(1, machine.boardLocations.size());
		}
	}

	@Test
	void testReadPythonFromCSV() {
		var f = getFile(CSV_DEFINED);
		try (var mdl = new MachineDefinitionConverter()) {
			var c = mdl.loadClassicConfigurationDefinition(f, true);
			assertEquals(1, c.machines.size());
			var machine = c.machines.get(0);
			assertNotNull(machine);
			assertEquals("SpiNNaker1M", machine.name);
			// This is actually cut down a lot from the real 1M machine
			assertEquals(3, machine.boardLocations.size());
			assertEquals(Set.of(new XYZ(0, 0, 0), new XYZ(0, 0, 1),
					new XYZ(0, 0, 2)), machine.boardLocations.keySet());
			assertEquals(1, machine.bmpIPs.size());
			assertEquals(3, machine.spinnakerIPs.size());
			assertEquals(Set.of("10.11.193.1", "10.11.193.17", "10.11.193.9"),
					new HashSet<>(machine.spinnakerIPs.values()));
			assertEquals(machine.boardLocations.keySet(),
					machine.spinnakerIPs.keySet());
		}
	}

	@Test
	void testProduceJSON() throws IOException, JSONException {
		var expectedJson = IOUtils.toString(getResource(EXPECTED_JSON), UTF_8);
		var f = getFile(SINGLE_BOARD);
		try (var mdl = new MachineDefinitionConverter()) {
			var c = mdl.loadClassicConfigurationDefinition(f, true);
			var json = getJsonWriter().writeValueAsString(c);
			assertNotNull(json);
			JSONAssert.assertEquals(expectedJson, json, STRICT);
		}
	}
}
