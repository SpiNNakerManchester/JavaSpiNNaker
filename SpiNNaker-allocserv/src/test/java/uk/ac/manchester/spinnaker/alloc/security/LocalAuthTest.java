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
package uk.ac.manchester.spinnaker.alloc.security;

import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.TestSupport;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.security.LocalAuthProviderImpl.TestAPI;

@SpringBootTest
@SpringJUnitWebConfig(TestSupport.Config.class)
@ActiveProfiles("unittest")
class LocalAuthTest extends TestSupport {

	private TestAPI authEngine;

	@BeforeEach
	@SuppressWarnings("deprecation")
	void checkSetup(
			@Autowired LocalAuthenticationProvider<TestAPI> authEngine)
	        throws IOException {
		assumeTrue(db != null, "spring-configured DB engine absent");
		killDB();
		setupDB1();
		this.authEngine = authEngine.getTestAPI();
	}

	// The actual tests

	@Test
	public void unlockUser() throws Exception {
		try (var c = db.getConnection()) {
			c.transaction(() -> {
				// 90k seconds is more than one day
				try (var setLocked =
						c.update("UPDATE user_info SET locked = :locked, "
								+ "last_fail_timestamp = :time - 90000 "
								+ "WHERE user_id = :user_id")) {
					setLocked.call(true, now(), USER);
				}
			});
		}

		authEngine.unlock();

		try (var c = db.getConnection()) {
			assertEquals(false, c.transaction(() -> {
				try (var q = c.query("SELECT locked FROM user_info "
						+ "WHERE user_id = :user_id")) {
					return q.call1(Row.bool("locked"), USER).orElseThrow();
				}
			}));
		}
	}
}
