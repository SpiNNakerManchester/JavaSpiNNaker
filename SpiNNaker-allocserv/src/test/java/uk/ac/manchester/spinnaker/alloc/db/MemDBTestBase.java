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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Support class for doing testing against an in-memory database. There will be
 * a connection per test (i.e., the DB will be implicitly deleted at the end of
 * <em>each</em> test), with that connection made available in the {@link #c}
 * field. That field <em>must not</em> be modified by subclasses.
 * <p>
 * Subclasses should be annotated with {@link SpringBootTest}.
 */
@UsedInJavadocOnly(SpringBootTest.class)
public abstract class MemDBTestBase extends SQLQueries {
	private DatabaseEngine memdb;

	/**
	 * The DB connection. Only valid in a test. <em>Must not</em> be modified by
	 * subclasses.
	 */
	@SuppressWarnings("checkstyle:visibilitymodifier")
	protected Connection c;

	@BeforeAll
	void getMemoryDatabase(@Autowired DatabaseEngine mainDBEngine) {
		assumeTrue(mainDBEngine != null, "spring-configured DB engine absent");
		memdb = mainDBEngine.getInMemoryDB();
	}

	@BeforeEach
	@SuppressWarnings("MustBeClosed")
	void getConnection() {
		c = memdb.getConnection();
		assumeTrue(c != null, "connection not generated");
	}

	@AfterEach
	void closeConnection() {
		c.close();
	}
}
