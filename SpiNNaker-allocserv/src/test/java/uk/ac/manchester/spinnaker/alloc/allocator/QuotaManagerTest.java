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
package uk.ac.manchester.spinnaker.alloc.allocator;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.TestSupport;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;

@SpringBootTest
@SpringJUnitWebConfig(TestSupport.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + QuotaManagerTest.DB,
	"spalloc.historical-data.path=" + QuotaManagerTest.HIST_DB
})
class QuotaManagerTest extends TestSupport {
	/** The DB file. */
	static final String DB = "target/qm_test.sqlite3";

	/** The DB file. */
	static final String HIST_DB = "target/qm_test_hist.sqlite3";

	/** Because the regular scheduled actions are not running. */
	private QuotaManager.TestAPI qm;

	@BeforeAll
	static void clearDB() throws IOException {
		killDB(DB);
	}

	@BeforeEach
	@SuppressWarnings("deprecation")
	void checkSetup(@Autowired QuotaManager qm) {
		assumeTrue(db != null, "spring-configured DB engine absent");
		setupDB1();
		this.qm = qm.getTestAPI();
	}

	private Long getQuota(Connection c) {
		try (var q = c.query(TEST_GET_QUOTA)) {
			return q.call1(MACHINE, USER).orElseThrow().getLong("quota");
		}
	}

	private void setQuota(Connection c, Integer quota) {
		try (var u = c.update(TEST_SET_QUOTA)) {
			u.call(quota, GROUP);
		}
	}

	/** Does a job get consolidated once and only once. */
	@Test
	public void consolidate() {
		checkAndRollback(c -> {
			int used = 100;
			makeFinishedJob(c, 1, used);
			assertEquals(INITIAL_QUOTA, getQuota(c));
			qm.doConsolidate(c);
			assertEquals(INITIAL_QUOTA - used, getQuota(c));
			qm.doConsolidate(c);
			assertEquals(INITIAL_QUOTA - used, getQuota(c));
		});
	}

	/** Does a job <em>not</em> get consolidated if there's no quota. */
	@Test
	public void noConsolidate() {
		checkAndRollback(c -> {
			// Delete the quota
			setQuota(c, null);
			makeFinishedJob(c, 1, 100);
			assertNull(getQuota(c));
			qm.doConsolidate(c);
			assertNull(getQuota(c));
		});
	}
}
