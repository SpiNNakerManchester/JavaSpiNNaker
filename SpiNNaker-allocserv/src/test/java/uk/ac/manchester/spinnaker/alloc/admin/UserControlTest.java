/*
 * Copyright (c) 2025 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.admin;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.TestSupport;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
import uk.ac.manchester.spinnaker.alloc.security.TrustLevel;

@SpringBootTest
@SpringJUnitWebConfig(TestSupport.Config.class)
@ActiveProfiles("unittest")
public class UserControlTest extends TestSupport {

	@Autowired
	private UserControl userControl;

	@BeforeEach
	void checkSetup() throws IOException {
		assumeTrue(db != null, "spring-configured DB engine absent");
		killDB();
	}

	@Test
	public void testCreateUser() {
		doTransactionalTest(() -> {
			UserRecord user = new UserRecord();
			user.setUserName("test");
			user.setEnabled(true);
			user.setHasPassword(true);
			user.setTrustLevel(TrustLevel.USER);
			user.setPassword("test");

			var userRecord = userControl.createUser(user,
					m -> URI.create(""));
			assertTrue(userRecord.isPresent());

			// The response is just a user sketch...
			var realUserRecord = userRecord.get();
			assertTrue(realUserRecord.getUserId() != null);
			assertEquals(user.getUserName(), realUserRecord.getUserName());
			assertEquals(null, realUserRecord.getOpenIdSubject());
		});
	}

	@Test
	public void testGetUserRecord() {
		doTransactionalTest(() -> {
			makeUser(conn);
			var record = userControl.getUser(USER, m -> URI.create(""));
			assertTrue(record.isPresent());
			var user = record.get();
			assertEquals(USER, user.getUserId());
			assertEquals(USER_NAME, user.getUserName());
		});
	}

	@Test
	public void testUpdateUser() {

		// Create a user
		UserRecord user = new UserRecord();
		user.setUserName("test");
		user.setEnabled(true);
		user.setHasPassword(true);
		user.setTrustLevel(TrustLevel.USER);
		user.setPassword("test");

		var userRecord = userControl.createUser(user, m-> URI.create(""));
		assertTrue(userRecord.isPresent());
		var id = userRecord.get().getUserId();

		// Create an admin
		UserRecord admin = new UserRecord();
		admin.setUserName("admin");
		admin.setEnabled(true);
		admin.setHasPassword(true);
		admin.setTrustLevel(TrustLevel.ADMIN);
		admin.setPassword("admin");

		var adminRecord = userControl.createUser(admin, m -> URI.create(""));
		assertTrue(adminRecord.isPresent());

		// Update the user password
		var retrievedUser = userControl.getUser(
				userRecord.get().getUserName(), (mr) -> {
			return URI.create("");
		}).get();
		retrievedUser.sanitise();
		retrievedUser.setPassword("newpassword");
		retrievedUser.setUserId(null);
		var result = userControl.updateUser(id, retrievedUser,
				admin.getUserName(), (mr) -> {return URI.create("");});
		assertTrue(result.isPresent());
	}

}
