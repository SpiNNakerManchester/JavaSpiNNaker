/*
 * Copyright (c) 2021-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.py2json;

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static java.io.File.createTempFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.ac.manchester.spinnaker.py2json.MachineDefinitionConverter.getJsonWriter;
import static uk.ac.manchester.spinnaker.py2json.MachineDefinitionConverter.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;

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

	private static final String THREE_BOARD = "three_board.py";

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
			assertEquals("192.168.0.2",
					machine.bmpIPs.get(new BMPCoords(0, 0)));
			assertEquals(new PhysicalCoords(0, 0, 0),
					machine.boardLocations.get(new TriadCoords(0, 0, 0)));
			assertEquals("192.168.0.3",
					machine.spinnakerIPs.get(new TriadCoords(0, 0, 0)));
			assertEquals(
					"Machine(name=my-board,tags=[default],width=1,height=1,"
							+ "deadBoards=[[x:0,y:0,z:1], [x:0,y:0,z:2]],"
							+ "deadLinks={},"
							+ "boardLocations={[x:0,y:0,z:0]=[c:0,f:0,b:0]},"
							+ "bmpIPs={[c:0,f:0]=192.168.0.2},"
							+ "spinnakerIPs={[x:0,y:0,z:0]=192.168.0.3})",
					machine.toString());
		}
	}

	@Test
	void testReadPythonThreeBoard() {
		var f = getFile(THREE_BOARD);
		try (var mdl = new MachineDefinitionConverter()) {
			var c = mdl.loadClassicConfigurationDefinition(f, false);
			var m = c.machines.get(0);
			assertNotNull(m);
			assertEquals(EnumSet.of(Link.east),
					m.deadLinks.get(new TriadCoords(0, 0, 0)));
			assertNotEquals("", c.toString());
		}
	}

	@Test
	void testReadPythonFromCSV() {
		var f = getFile(CSV_DEFINED);
		try (var mdl = new MachineDefinitionConverter()) {
			var c = mdl.loadClassicConfigurationDefinition(f, true);
			mdl.validate(c);
			assertEquals(1, c.machines.size());
			var machine = c.machines.get(0);
			assertNotNull(machine);
			assertEquals("SpiNNaker1M", machine.name);
			// This is actually cut down a lot from the real 1M machine
			assertEquals(3, machine.boardLocations.size());
			assertEquals(
					Set.of(new TriadCoords(0, 0, 0), new TriadCoords(0, 0, 1),
							new TriadCoords(0, 0, 2)),
					machine.boardLocations.keySet());
			assertEquals(1, machine.bmpIPs.size());
			assertEquals(3, machine.spinnakerIPs.size());
			assertEquals(Set.of("10.11.193.1", "10.11.193.17", "10.11.193.9"),
					Set.copyOf(machine.spinnakerIPs.values()));
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

	@Test
	void checkMain() throws Exception {
		var expectedJson = IOUtils.toString(getResource(EXPECTED_JSON), UTF_8);
		var src = getFile(SINGLE_BOARD);
		var dst = createTempFile("dst", ".json");
		try {
			catchSystemExit(
					() -> main(src.getAbsolutePath(), dst.getAbsolutePath()));

			assertTrue(dst.exists());
			try (var r = new BufferedReader(new FileReader(dst, UTF_8))) {
				JSONAssert.assertEquals(expectedJson, r.readLine(), true);
			}
		} finally {
			dst.delete();
			if (dst.exists()) {
				dst.deleteOnExit();
			}
		}
	}
}
