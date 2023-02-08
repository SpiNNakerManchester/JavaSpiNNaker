/*
 * Copyright (c) 2021 The University of Manchester
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
