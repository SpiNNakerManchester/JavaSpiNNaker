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
package uk.ac.manchester.spinnaker.alloc.bmp;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.BOARD;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.setupDB1;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl;
import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl.BoardState;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;
import uk.ac.manchester.spinnaker.utils.OneShotEvent;

@SpringBootTest
@SpringJUnitWebConfig(BlacklistCommsTest.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + BlacklistCommsTest.DB,
	"spalloc.historical-data.path=" + BlacklistCommsTest.HIST_DB
})
class BlacklistCommsTest extends SQLQueries {
	/** The DB file. */
	static final String DB = "target/blcomms_test.sqlite3";

	/** The DB file. */
	static final String HIST_DB = "target/blcomms_test_hist.sqlite3";

	private static final Logger log = getLogger(BlacklistCommsTest.class);

	@Configuration
	@ComponentScan(basePackageClasses = SpallocProperties.class)
	static class Config {
	}

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private MachineStateControl stateCtrl;

	@Autowired
	private BMPController bmpCtrl;

	@BeforeAll
	static void clearDB() throws IOException {
		Path dbp = Paths.get(DB);
		if (exists(dbp)) {
			log.info("deleting old database: {}", dbp);
			delete(dbp);
		}
	}

	@BeforeEach
	void checkSetup() {
		assumeTrue(db != null, "spring-configured DB engine absent");
		try (Connection c = db.getConnection()) {
			c.transaction(() -> setupDB1(c));
		}
	}

	@Test
	@Timeout(15)
	@SuppressWarnings("deprecation") // Calling internal API
	public void readBlacklistFromSystem() throws Exception {
		// This is messy; can't have a transaction open and roll it back
		BoardState bs = stateCtrl.findId(BOARD).get();
		ExecutorService exec = newSingleThreadExecutor();
		OneShotEvent ready = new OneShotEvent();
		try {
			Future<?> future = exec.submit(() -> {
				ready.fire();
				Thread.sleep(1000);
				bmpCtrl.getTestAPI().processRequests(2000);
				return "BMP done";
			});
			ready.await();
			Blacklist bl = stateCtrl.pullBlacklist(bs).get();
			assertEquals("BMP done", future.get());
			assertEquals(new Blacklist("chip 5 5 core 5"), bl);
		} finally {
			exec.shutdown();
		}
	}
}
