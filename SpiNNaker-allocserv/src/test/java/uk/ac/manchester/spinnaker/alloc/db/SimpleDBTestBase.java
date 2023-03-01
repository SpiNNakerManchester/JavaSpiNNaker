/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.db;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;

import uk.ac.manchester.spinnaker.alloc.TestSupport;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Support class for doing testing against a simple database. There will be
 * a connection per test and the DB will be implicitly cleaned at the end of
 * <em>each</em> test, with that connection made available in the {@link #c}
 * field. That field <em>must not</em> be modified by subclasses.
 * <p>
 * Subclasses should be annotated with {@link SpringBootTest}.
 */
@UsedInJavadocOnly(SpringBootTest.class)
public abstract class SimpleDBTestBase extends TestSupport {
	/**
	 * The DB connection. Only valid in a test. <em>Must not</em> be modified by
	 * subclasses.
	 */
	@SuppressWarnings("checkstyle:visibilitymodifier")
	protected Connection c;

	@BeforeEach
	@SuppressWarnings("MustBeClosed")
	void getConnection() throws IOException {
		c = db.getConnection();
		assumeTrue(c != null, "connection not generated");
		killDB();
	}

	@AfterEach
	void closeConnection() {
		c.close();
	}
}
