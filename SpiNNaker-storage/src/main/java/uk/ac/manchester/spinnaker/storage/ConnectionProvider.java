/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Indicates a class that can provide database connections to
 * suitably-configured databases.
 *
 * @author Donal Fellows
 * @param <APIType>
 *            The type of the higher-level access interface that can be used to
 *            work with the database this class makes connections to.
 */
public interface ConnectionProvider<APIType extends DatabaseAPI> {
	/**
	 * Get a connection to a database, creating it if needed.
	 *
	 * @return The configured connection to the database. The database will have
	 *         been seeded with DDL if necessary.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	Connection getConnection() throws SQLException;

	/**
	 * @return a storage interface that is suitable for providing support for a
	 *         particular API.
	 */
	APIType getStorageInterface();
}
