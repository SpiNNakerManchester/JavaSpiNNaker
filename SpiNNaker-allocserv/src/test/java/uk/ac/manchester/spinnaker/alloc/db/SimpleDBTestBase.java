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
package uk.ac.manchester.spinnaker.alloc.db;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
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
