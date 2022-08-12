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
package uk.ac.manchester.spinnaker.alloc.compat;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.setupDB1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;

@SpringBootTest
@SpringJUnitWebConfig(V1CompatTest.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + V1CompatTest.DB,
	"spalloc.historical-data.path=" + V1CompatTest.HIST_DB,
	"spalloc.compat.thread-pool-size=10"
})
class V1CompatTest extends SQLQueries {
	private static final Logger log = getLogger(V1CompatTest.class);

	/** The name of the database file. */
	static final String DB = "target/compat_test.sqlite3";

	/** The name of the database file. */
	static final String HIST_DB = "target/compat_test-hist.sqlite3";

	@Configuration
	@ComponentScan(basePackageClasses = SpallocProperties.class)
	static class Config {
	}

	@Autowired
	private DatabaseEngine db;

	private V1CompatService.TestAPI testAPI;

	@BeforeAll
	static void clearDB() throws IOException {
		Path dbp = Paths.get(DB);
		if (exists(dbp)) {
			log.info("deleting old database: {}", dbp);
			delete(dbp);
		}
	}

	@BeforeEach
	@SuppressWarnings("deprecation")
	void checkSetup(@Autowired V1CompatService compat) {
		assumeTrue(db != null, "spring-configured DB engine absent");
		try (Connection c = db.getConnection()) {
			c.transaction(() -> setupDB1(c));
		}
		testAPI = compat.getTestApi();
	}

	private void withInstance(
			BiConsumer<PrintWriter, NonThrowingLineReader> act)
			throws Exception {
		try (PipedWriter to = new PipedWriter();
				PipedReader from = new PipedReader()) {
			Future<?> f = testAPI.launchInstance(to, from);
			try {
				act.accept(new PrintWriter(to),
						new NonThrowingLineReader(from));
			} finally {
				if (!f.cancel(true)) {
					System.err.println("cancel failed?");
				}
			}
		}
	}

	// The actual tests

	@Test
	public void testMachineryTest() throws Exception {
		for (int i = 0; i < 100; i++) {
			withInstance((to, from) -> {
				to.println();
				from.readLine();
			});
		}
		for (int i = 0; i < 100; i++) {
			withInstance((to, from) -> {
				to.println();
			});
		}
	}

	@Test
	public void listMachines() throws Exception {
		String machinesResponse =
				"{\"return\":[{\"name\":\"foo_machine\",\"tags\":[],"
						+ "\"width\":1,\"height\":1,\"dead_boards\":[],"
						+ "\"dead_links\":[]}]}";
		withInstance((to, from) -> {
			to.println("{\"command\": \"list_machines\"}");
			assertEquals(machinesResponse, from.readLine());
			to.println("{\"command\": \"list_machines\", \"args\": [0]}");
			assertEquals(machinesResponse, from.readLine());
			// An exception
			to.println("{\"command\": \"list_machines\", \"args\": false}");
			String line = from.readLine();
			assertNotEquals(machinesResponse, line);
			assertTrue(line.startsWith("{\"exception\":"),
					() -> "expected exception in " + line);
		});
	}

	@Test
	public void listJobs() throws Exception {
		String jobsResponse = "{\"return\":[]}";
		withInstance((to, from) -> {
			to.println("{\"command\": \"list_jobs\"}");
			assertEquals(jobsResponse, from.readLine());
		});
	}
}

/** A buffered reader that doesn't throw if it gets an error reading a line. */
class NonThrowingLineReader extends BufferedReader {
	NonThrowingLineReader(PipedReader r) {
		super(r);
	}

	@Override
	public String readLine() {
		try {
			return super.readLine();
		} catch (IOException e) {
			return "THREW " + e;
		}
	}
}
