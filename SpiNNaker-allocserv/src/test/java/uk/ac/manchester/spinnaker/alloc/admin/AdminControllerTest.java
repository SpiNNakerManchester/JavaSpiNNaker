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

import static uk.ac.manchester.spinnaker.alloc.admin.AdminController.BASE_PATH;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminController.CREATE_USER_PATH;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminController.USERS_PATH;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.net.URI;
import java.util.Base64;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import uk.ac.manchester.spinnaker.alloc.TestSupport;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
import uk.ac.manchester.spinnaker.alloc.security.TrustLevel;

@AutoConfigureMockMvc
@SpringBootTest
@SpringJUnitWebConfig(TestSupport.Config.class)
@ActiveProfiles("unittest")
public class AdminControllerTest extends TestSupport {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserControl userControl;

	@Test
	public void testCreateUser() throws Exception {
		doTransactionalTest(() -> {
			// Create an admin
			UserRecord admin = new UserRecord();
			admin.setUserName("admin");
			admin.setEnabled(true);
			admin.setHasPassword(true);
			admin.setTrustLevel(TrustLevel.ADMIN);
			admin.setPassword("admin");

			var adminRecord = userControl.createUser(admin,
					m -> URI.create(""));
			assertTrue(adminRecord.isPresent());

			String userPass = Base64.getEncoder().encodeToString(
					"admin:admin".getBytes());
			try {
				var result = mvc.perform(
					MockMvcRequestBuilders
					.post(BASE_PATH + CREATE_USER_PATH)
					.with(csrf())
					.header("Authorization", "Basic " + userPass)
					.param("internal", "true")
					.param("userName", "test")
					.param("trustLevel", "USER")
					.param("password", "test")
					.param("enabled", "true")
					.param("hasPassword", "true")
					.accept(MediaType.APPLICATION_JSON));
				result.andExpect(
						redirectedUrlPattern(BASE_PATH + USERS_PATH + "/*"));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	public void updateUserPassword() throws Exception {
		doTransactionalTest(() -> {
			// Create an admin
			UserRecord admin = new UserRecord();
			admin.setUserName("admin");
			admin.setEnabled(true);
			admin.setHasPassword(true);
			admin.setTrustLevel(TrustLevel.ADMIN);
			admin.setPassword("admin");

			var adminRecord = userControl.createUser(admin,
					m -> URI.create(""));
			assertTrue(adminRecord.isPresent());
			log.info("Admin created: {}", adminRecord.get());

			String userPass = Base64.getEncoder().encodeToString(
					"admin:admin".getBytes());

			// Create a user
			UserRecord test = new UserRecord();
			test.setUserName("test");
			test.setEnabled(true);
			test.setHasPassword(true);
			test.setTrustLevel(TrustLevel.USER);
			test.setPassword("test");

			var testRecord = userControl.createUser(test,
					m -> URI.create(""));
			assertTrue(testRecord.isPresent());
			UserRecord testUser = testRecord.get();

			try {
				var result = mvc.perform(
					MockMvcRequestBuilders
					.post(BASE_PATH + USERS_PATH + "/" + testUser.getUserId())
					.with(csrf())
					.header("Authorization", "Basic " + userPass)
					.param("internal", "true")
					.param("userName", "test")
					.param("trustLevel", "USER")
					.param("password", "testchange")
					.accept(MediaType.APPLICATION_JSON));

				// Note: not sure how to get this dynamically!
				result.andExpect(
					forwardedUrl("/WEB-INF/views/admin/userdetails.jsp"));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}


}
